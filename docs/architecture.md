# URL Shortener — Architecture & Design Diagrams

This document describes the **target** architecture for the MVP (see [OpenSpec change](../openspec/changes/dynamodb-redis-url-shortener-mvp/) for requirements). Diagrams use [Mermaid](https://mermaid.js.org/) and render on GitHub.

---

## 1. System context

External actors and the single deployable system boundary.

```mermaid
flowchart TB
  subgraph actors["Actors"]
    Browser[Web browsers / mobile clients]
    APIClient[API clients]
  end

  subgraph system["URL Shortener system"]
    API[Spring Boot service]
  end

  Browser -->|HTTPS redirect GET /shortCode| API
  APIClient -->|HTTPS JSON| API
```

---

## 2. AWS deployment (ECS Fargate)

Reference layout for **`us-east-1`**: TLS at the load balancer, tasks in a VPC, regional DynamoDB, Redis in-VPC.

```mermaid
flowchart TB
  subgraph clients["Clients"]
    U[Users]
  end

  subgraph aws["AWS us-east-1"]
    subgraph edge["Edge"]
      ALB[Application Load Balancer\nTLS termination]
    end

    subgraph vpc["VPC"]
      subgraph ecs["ECS Fargate"]
        T1[Spring Boot task]
        T2[Spring Boot task]
      end
      Redis[(ElastiCache Redis)]
    end

    DDB[(DynamoDB)]
    CW[Amazon CloudWatch]
    SM[Secrets Manager / SSM\noptional: Redis AUTH]

    U --> ALB
    ALB -->|HTTP :8080| T1
    ALB -->|HTTP :8080| T2
    T1 --> Redis
    T2 --> Redis
    T1 --> DDB
    T2 --> DDB
    T1 -. Micrometer metrics .-> CW
    T2 -. Micrometer metrics .-> CW
    ecs -. read secrets at start .-> SM
  end
```

**Security groups (typical):**

- ALB: allow `443` (and optionally `80`) from the internet or your edge.
- ECS tasks: allow app port from **ALB SG** only; allow **outbound** to DynamoDB (via IGW/NAT or gateway endpoint) and to **Redis SG** on `6379`.
- Redis: allow **6379** from **ECS task SG** only.

---

## 3. Logical components (inside the service)

Responsibilities layered for clarity; exact package names may differ in code.

```mermaid
flowchart TB
  subgraph api["API layer"]
    REST["REST controllers\nPOST /api/v1/urls, GET /shortCode, analytics"]
    VAL[Validation / blocked domains]
    RL[Rate limiting]
  end

  subgraph app["Application layer"]
    SVC[Short URL service]
    GEN[Code generator\n7 x a-z0-9]
  end

  subgraph infra["Infrastructure adapters"]
    CACHE[Redis cache adapter\nread-through, TTL, optional negative cache]
    REPO[DynamoDB repository\nconditional writes, updates]
  end

  subgraph obs["Observability"]
    MET[Micrometer\nlatency, errors, cache hits]
  end

  REST --> RL
  RL --> VAL
  VAL --> SVC
  SVC --> GEN
  SVC --> CACHE
  SVC --> REPO
  SVC --> MET
  CACHE --> REPO
```

---

## 4. Data flow — create short URL

```mermaid
sequenceDiagram
  actor C as Client
  participant ALB as Load balancer
  participant S as Spring Boot
  participant D as DynamoDB
  participant R as Redis

  C->>ALB: POST /api/v1/urls
  ALB->>S: forward
  S->>S: validate URL, TTL, blocked domains
  S->>S: generate shortCode (or use alias)
  S->>D: PutItem (conditional: code must not exist)
  alt alias or random collision
    D-->>S: ConditionalCheckFailed
    S->>S: retry random / return 409 for alias
  else success
    D-->>S: OK
    S->>R: optional cache warm / invalidate
    S-->>C: 201 + shortCode
  end
```

---

## 5. Data flow — redirect (cache read-through)

```mermaid
sequenceDiagram
  actor B as Browser
  participant ALB as Load balancer
  participant S as Spring Boot
  participant R as Redis
  participant D as DynamoDB

  B->>ALB: GET /shortCode
  ALB->>S: forward
  S->>S: validate code shape [a-z0-9] length 7
  S->>R: GET Redis url key for shortCode
  alt cache hit
    R-->>S: cached mapping + expiry meta
  else cache miss
    R-->>S: (null)
    S->>D: GetItem(shortCode)
    D-->>S: item or not found
    S->>R: SET Redis url key with TTL
  end
  S->>S: apply biz rules (missing 404, expired 410, active 302)
  alt active mapping
    S->>S: update analytics (total, last access, hourly bucket)
    S-->>B: 302 Location: originalUrl
  else not found / expired
    S-->>B: 404 / 410
  end
```

---

## 6. Data flow — analytics read

```mermaid
sequenceDiagram
  actor C as Client
  participant S as Spring Boot
  participant D as DynamoDB

  C->>S: GET /api/v1/urls/shortCode/analytics
  S->>D: GetItem(shortCode)
  D-->>S: mapping + rollup fields
  S-->>C: 200 JSON: totalClicks, lastAccessedAt, hourly buckets (≤30d)
  S-->>C: 404 / 410 when applicable
```

---

## 7. DynamoDB item (conceptual)

Single-table style MVP: **partition key** = `shortCode` (String). Attributes are illustrative; names may be adjusted in implementation.

| Attribute | Role |
|-----------|------|
| `shortCode` | PK |
| `originalUrl` | Redirect target |
| `createdAt` | Audit |
| `expiresAt` | App-level `410` + DynamoDB TTL attribute |
| `totalClicks` | Rollup counter |
| `lastAccessedAt` | Rollup timestamp |
| `hourlyBuckets` | Map or sparse attributes; **30-day** retention policy |
| `ttl` | DynamoDB TTL epoch seconds (if named differently in console, align with app) |

---

## 8. Redis keys (conceptual)

| Key | Value | Notes |
|-----|--------|--------|
| `url:{shortCode}` | Serialized URL + expiry metadata | Read-through on redirect; bounded TTL |
| Optional `miss:{shortCode}` | Sentinel for negative caching | Short TTL to reduce DB churn |

---

## Related documents

- [README](../README.md) — product summary and API table  
- [OpenSpec design](../openspec/changes/dynamodb-redis-url-shortener-mvp/design.md) — decisions and risks  
- [OpenSpec delta spec](../openspec/changes/dynamodb-redis-url-shortener-mvp/specs/url-shortening/spec.md) — normative behaviour
