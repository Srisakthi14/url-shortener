# Delta for url-shortening

## ADDED Requirements

### Requirement: Create short URL

The system SHALL accept `POST /api/v1/urls` from anonymous clients and create a new short mapping every request, even when the `originalUrl` duplicates an existing mapping. The system SHALL generate a random `shortCode` consisting of exactly seven characters from the charset `[a-z0-9]`. The system SHALL allow an optional `customAlias` that becomes the `shortCode` when supplied; if that code already exists, the system SHALL respond with `409 Conflict` and MUST NOT overwrite the existing mapping. The system SHALL validate `originalUrl` (scheme allowed, reachable length limits, syntactic validity) and reject invalid input with `400 Bad Request`. The system SHALL reject requests whose target host matches the configured blocked-domain list with `400 Bad Request`. The system SHALL set `expiresAt` to creation time plus the requested TTL when provided, otherwise default to thirty days, and MUST NOT accept TTL greater than three hundred sixty-five days from creation. The system SHALL persist the mapping as the authoritative record in DynamoDB.

#### Scenario: Successful creation with defaults

- **WHEN** a client posts a valid `https` URL without custom alias or TTL
- **THEN** the system responds `201 Created`
- **AND** the response body includes a new seven-character `shortCode` from `[a-z0-9]`
- **AND** the mapping expires thirty days after creation

#### Scenario: Custom alias conflict

- **WHEN** a client posts a custom alias that already exists
- **THEN** the system responds `409 Conflict`
- **AND** no existing mapping is modified

#### Scenario: Blocked domain

- **WHEN** a client posts a URL whose host is on the static blocked-domain list
- **THEN** the system responds `400 Bad Request`

### Requirement: Redirect by short code

The system SHALL expose `GET /{shortCode}` for public resolution. Before storage lookup, the system SHALL treat codes containing characters outside `[a-z0-9]` or whose length is not exactly seven as invalid format and respond with `400 Bad Request`. For valid format, the system SHALL attempt read-through resolution: on cache hit, use cached metadata; on miss, load from DynamoDB and populate the cache. If no mapping exists, the system SHALL respond `404 Not Found`. If a mapping exists but `now` is after `expiresAt`, the system SHALL respond `410 Gone`. If a mapping exists and is not expired, the system SHALL respond with `302 Found` and a `Location` header set to the stored `originalUrl`. The system SHALL emit metrics suitable for measuring cache hit ratio and redirect latency.

#### Scenario: Active mapping

- **WHEN** a client requests a valid code that exists and is not expired
- **THEN** the system responds `302 Found` with `Location` equal to the stored original URL

#### Scenario: Missing mapping

- **WHEN** a client requests a valid code that does not exist
- **THEN** the system responds `404 Not Found`

#### Scenario: Expired mapping

- **WHEN** a client requests a valid code that exists but is past `expiresAt`
- **THEN** the system responds `410 Gone`

#### Scenario: Invalid code format

- **WHEN** a client requests a path shorter or longer than seven characters or containing characters outside `[a-z0-9]`
- **THEN** the system responds `400 Bad Request`

### Requirement: Bounded analytics

The system SHALL expose `GET /api/v1/urls/{shortCode}/analytics` for valid seven-character `[a-z0-9]` identifiers. If the code does not exist, the system SHALL respond `404 Not Found`. If it exists but is expired, the system SHALL respond `410 Gone`. For active mappings, the system SHALL return total click count, `lastAccessedAt` timestamp, and hourly click counts covering at most the last thirty days. The system MUST NOT store raw per-click events in MVP. Analytics accuracy MAY be eventually consistent.

#### Scenario: Analytics for active mapping

- **WHEN** a client requests analytics for an active short code
- **THEN** the system responds `200 OK` with total clicks, `lastAccessedAt`, and hourly bucket totals not older than thirty days

#### Scenario: Analytics for missing mapping

- **WHEN** a client requests analytics for an unknown short code
- **THEN** the system responds `404 Not Found`

#### Scenario: Analytics for expired mapping

- **WHEN** a client requests analytics for an expired short code
- **THEN** the system responds `410 Gone`

### Requirement: Anonymous abuse controls

The system SHALL apply per-IP rate limits to creation and redirect endpoints sufficient to mitigate abusive bursts at MVP scale targets. The system SHALL enforce blocked-domain rejection on create. The system SHALL rely on static configuration for the blocked-domain list in this release.

#### Scenario: Rate limit exceeded

- **WHEN** a single client IP exceeds the configured rate limit for a protected endpoint
- **THEN** the system responds `429 Too Many Requests`

### Requirement: Observability

The system SHALL expose application metrics compatible with Micrometer for request latency, error rates, dependency health (DynamoDB and Redis), and cache effectiveness. The system SHALL publish or scrape metrics so operators can monitor them in Amazon CloudWatch.

#### Scenario: Operators inspect redirect latency

- **WHEN** the system processes redirect traffic under load tests
- **THEN** metrics include percentile latency series suitable for asserting p95 under one hundred milliseconds at target throughput
