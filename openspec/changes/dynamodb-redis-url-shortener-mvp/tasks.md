## 1. Foundation and configuration

- [x] 1.1 Add Spring configuration properties for DynamoDB table name(s), Redis connection, rate-limit thresholds, max URL length, and blocked-domain list (static)
- [x] 1.2 Add AWS SDK v2 DynamoDB enhanced client and Lettuce/Spring Data Redis wiring for `us-east-1`
- [x] 1.3 Document local/dev setup (e.g., Testcontainers or placeholders) for integration tests

## 2. Data model and persistence

- [x] 2.1 Define DynamoDB entity: `shortCode` (partition key), `originalUrl`, `createdAt`, `expiresAt` (TTL attribute), `totalClicks`, `lastAccessedAt`, hourly bucket map (or item collection pattern), optional `isCustomAlias` flag
- [x] 2.2 Implement repository: conditional `PutItem` for create (random retries on collision); `GetItem` for load by code
- [x] 2.3 Implement hourly bucket trim logic to retain at most thirty rolling days of counts
- [x] 2.4 Implement atomic `UpdateItem` for redirect analytics: increment total, set `lastAccessedAt`, increment current UTC hour bucket

## 3. Short code generation

- [x] 3.1 Implement secure random generator for seven-character `[a-z0-9]` codes with bounded retry on conditional failure
- [x] 3.2 Implement custom alias validation (charset + length seven) and map to same persistence path

## 4. Caching

- [x] 4.1 Implement read-through cache: Redis key `url:{shortCode}` with value and TTL; populate on miss; optional short TTL for negative cache on miss
- [x] 4.2 Implement cache invalidation or overwrite on create; document Redis-down fallback to DynamoDB-only reads

## 5. HTTP API

- [x] 5.1 Implement `POST /api/v1/urls` with request/response DTOs, default thirty-day and max three-hundred-sixty-five-day TTL validation, blocked-domain check, duplicate URL always creates new code
- [x] 5.2 Implement `GET /{shortCode}` with format validation (`400`), existence (`404`), expiry (`410`), redirect `302` with `Location`
- [x] 5.3 Implement `GET /api/v1/urls/{shortCode}/analytics` with `404`/`410` semantics and thirty-day hourly rollup payload
- [x] 5.4 Map validation and conflict failures to `400`, `409` per spec

## 6. Abuse controls

- [x] 6.1 Add per-IP rate limiting filter for create and redirect paths; return `429 Too Many Requests` when exceeded
- [x] 6.2 Wire static blocked-domain list into create validation

## 7. Observability

- [x] 7.1 Add Micrometer timers/counters for create, redirect, analytics; tag cache hit/miss on redirect
- [x] 7.2 Expose metrics for DynamoDB and Redis health suitable for CloudWatch dashboards and alarms

## 8. Verification

- [x] 8.1 Add unit tests for code generation, TTL validation, and HTTP status mapping
- [x] 8.2 Add integration tests against DynamoDB/Redis test doubles for create → redirect → analytics flow
- [x] 8.3 Run load test harness to assert redirect p95 under one hundred milliseconds at five thousand reads per second (adjust local target proportionally if hardware-limited)
