## Why

We need a production-shaped URL shortener MVP that meets predictable latency for redirects, durable storage for mappings, and cost-aware analytics—without building auth or raw event pipelines yet. The project aligns implementation with clear behavioral contracts so Spring Boot, DynamoDB, Redis, and observability can evolve together.

## What Changes

- Add HTTP API: create short URLs, resolve redirects, and read analytics.
- Introduce random 7-character short codes over `[a-z0-9]`, with optional custom aliases.
- Enforce default link TTL (30 days) and maximum TTL (365 days); return `410 Gone` when expired.
- Use DynamoDB as source of truth for mappings, TTL, and rollup analytics metadata.
- Use Redis as a read-through cache for hot redirect lookups.
- Add MVP abuse controls: IP rate limiting, blocked domains from static config, and URL validation.
- Instrument with Micrometer and integrate with CloudWatch for operational visibility.
- Add workload targets: ~50 writes/sec, ~5,000 reads/sec, redirect p95 < 100ms.

## Capabilities

### New Capabilities

- `url-shortening`: End-to-end shortening—create mappings (including custom alias and collision semantics), redirect resolution with strict HTTP status behavior, validation and abuse controls, and bounded analytics (total clicks, last accessed, hourly buckets retained 30 days). No raw per-click events in MVP.

### Modified Capabilities

- _(none — no existing OpenSpec specs in this repository)_

## Impact

- **Application**: New Spring Boot modules or packages for API controllers, validation, DynamoDB/Redis clients, and observability hooks.
- **Infrastructure**: DynamoDB table(s) and TTL configuration, Redis/Elastiache (or equivalent) in `us-east-1`, IAM roles, VPC/security groups as applicable.
- **APIs**: Public anonymous REST endpoints under `/api/v1/...` and a top-level `GET /{shortCode}` redirect route.
- **Dependencies**: AWS SDK for DynamoDB, Redis client, rate limiting library or filter, Micrometer/CloudWatch exporter wiring.
