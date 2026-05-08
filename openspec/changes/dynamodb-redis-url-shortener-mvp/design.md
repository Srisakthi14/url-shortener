## Context

The repository is a brownfield Spring Boot application. The change adds a public, anonymous URL shortener API backed by **DynamoDB** (durability, TTL) and **Redis** (hot-path cache) in **us-east-1**. Product decisions are fixed: 7-character random codes over `[a-z0-9]`, optional custom aliases, default 30-day expiry (max 365 days), duplicate long URLs always produce new codes, and analytics are rollup-only (total count, last access, hourly buckets for 30 days)—no raw click events in MVP.

## Goals / Non-Goals

**Goals:**

- Meet redirect **p95 < 100ms** under **~5,000 reads/sec** and sustain **~50 writes/sec** with observable bottlenecks.
- Enforce explicit HTTP semantics (`302` active, `410` expired, `404` missing, `400` invalid format, `409` alias conflict).
- Keep data model and cache behavior simple: DynamoDB authoritative, Redis read-through for redirects.
- Operational visibility via **Micrometer + CloudWatch** (latency, errors, cache hit ratio, dependency health).

**Non-Goals:**

- Authentication, API keys, or multi-tenancy (post-MVP).
- Real-time analytics, per-click event storage, or long-term hourly retention beyond **30 days**.
- Dynamic blocked-domain administration UI (static config for MVP).

## Decisions

| Decision | Choice | Rationale | Alternatives considered |
|----------|--------|-----------|-------------------------|
| Redirect status | `302 Found` | Allows future target URL changes without sticky permanent redirects. | `301` — better for SEO on public shorteners but wrong for mutable targets. |
| Short code generation | Cryptographically strong random, Base36 lowercase, length 7 | Matches product charset; ~78B space reduces collision rate. | Counter + encode — needs global atomic counter; hash-based — complicates “always new code”. |
| Collision handling | Retry generation on conditional write failure | DynamoDB conditional put on short code ensures uniqueness. | Single try — unacceptable conflict rate under load tests. |
| Custom alias | Stored as primary short code value; same uniqueness rules | One code namespace keeps routing simple. | Separate alias table — more queries. |
| Expiry enforcement | App checks `expiresAt` before redirect; DynamoDB TTL attribute for cleanup | Immediate `410` even if TTL deletion lags. | TTL only — users could see stale success briefly. |
| Redis cache | Key `url:{code}`; value includes target URL + expiry metadata; bounded TTL | Read-through lowers DynamoDB RCU on hot keys. | Write-through only — higher write amplification. |
| Cache miss / negative cache | Cache “not found” briefly on `404` path | Protects against scan/abuse. | No negative cache — more DB load. |
| Analytics updates | On successful redirect: increment total atomically; set `lastAccessedAt`; increment current hour bucket | Matches “not real-time” SLA while keeping bounded storage. | Stream to Firehose — non-goal for MVP. |
| Hourly buckets | Composite key or map attribute; retain 30 rolling days | Product decision; trim old buckets on write or scheduled job. | Unbounded history — cost risk. |
| Rate limiting | Per-IP token bucket at edge or filter layer | Mitigates abuse for anonymous API. | Per-user limits — no users in MVP. |
| Blocked domains | Static list in config (YAML/env) | MVP simplicity. | DB-driven admin — later. |

## Risks / Trade-offs

- **[Risk] Hot-key write contention on extremely popular links** → **Mitigation**: Total counters and hourly buckets can become hot; consider DynamoDB `UpdateItem` with idempotency, sharded counters, or capping update frequency if metrics show throttling. MVP accepts best-effort counter accuracy under extreme fan-in.
- **[Risk] Redis outage increases DynamoDB load** → **Mitigation**: Degrade gracefully to DynamoDB; auto-scale/on-demand capacity; alert on RCU spike.
- **[Risk] Clock skew for hourly buckets** → **Mitigation**: Use UTC wall-clock buckets from trusted server time; document assumption.
- **[Risk] Alias / random namespace collisions** → **Mitigation**: Conditional writes + retry random generation; return `409` only for client-supplied alias conflicts.

## Migration Plan

1. Land application code behind feature flags or deploy with endpoints enabled only in non-prod first.
2. Create DynamoDB table(s), GSIs if needed (see implementation tasks), enable TTL on attribute.
3. Provision Redis; verify connectivity from Spring environment.
4. Deploy; smoke test create → redirect → analytics; monitor p95 and error budget.
5. Rollback: disable route or revert deployment; DynamoDB remains source of truth—no client data migration required for MVP.

## Open Questions

- Exact maximum body sizes and URL length limits for `originalUrl` (pick conservative defaults in implementation tasks).
- Whether to expose a health/readiness check that validates both DynamoDB and Redis or treat Redis as optional for “ready” (recommend: app live with Redis degraded, separate metric).
