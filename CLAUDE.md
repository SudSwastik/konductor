# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all Java modules from root
mvn clean install -DskipTests

# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl konductor-model
mvn test -pl konductor-publisher-sdk
mvn test -pl konductor-server

# Run a single test class
mvn test -pl konductor-server -Dtest=JsonPathFilterServiceTest

# Run integration tests (requires Docker for Testcontainers)
mvn verify -pl konductor-server

# Compile without running tests
mvn compile

# UI (Next.js)
cd konductor-ui && npm run dev    # dev server on :3000
cd konductor-ui && npm run build  # production build
cd konductor-ui && npm test       # Jest unit tests
```

## Architecture Overview

Three-module Maven project + one Next.js app. See `IMPLEMENTATION_PLAN.md` for the phased build plan.

```
konductor/
├── konductor-model/          # Shared POJOs — no Spring/Kafka deps
├── konductor-publisher-sdk/  # Producer client library
├── konductor-server/         # Spring Boot app (consumer + REST API)
└── konductor-ui/             # Next.js 14 ops/admin UI (TypeScript + SCSS)
```

### Event Envelope (3 parts)

Every Kafka message is a `KonductorEvent` with exactly three sections:

1. **`metadata`** (`EventMetadata`) — fixed schema, versioned. Fields: `eventId` (UUID), `eventType`, `eventTimestamp`, `sourceConfigId`, `configType`, `schemaVersion`, `correlationId`, `traceId`.
2. **`payload`** (`Map<String, Object>`) — fully dynamic JSON object. Validated against Confluent Schema Registry when toggle is ON.
3. **`dataRefs`** (`List<DataRef>`, nullable) — external large-payload pointers. Each has `uri`, `contentType`, `checksum` (SHA-256), `sizeBytes`.

### Schema Registry Toggle

Global flag in `application.yml`:
```yaml
konductor:
  schema-registry:
    enabled: true   # false = skip validation entirely
    url: http://schema-registry:8081
  kafka:
    topic: konductor-events
```

When `enabled=true`, the SDK validates `payload` against the registered JSON Schema for `(eventType, schemaVersion)` **before** publishing to Kafka. The toggle is wired via `@ConditionalOnProperty` — no if/else in business code.

### Subscription Registry (PostgreSQL)

Consumers request subscriptions specifying `eventType` and a list of JSONPath expressions (`field_paths`) for the payload fields they want. Subscriptions start `active=false` and require admin approval before they receive events.

Key tables:
- `subscriptions` — consumerId, eventType, field_paths (JSONB), active, subscription_version
- `consumer_events` — polling inbox; one row per (consumer, event); holds `filtered_payload` (JSONB)

### Event Processing Flow

```
Kafka @KafkaListener
  → load active subscriptions by eventType  [PostgreSQL]
  → for each subscription:
      1. Redis SETNX "dedup:{consumerId}:{eventId}" TTL=24h  → skip if exists
      2. Apply JSONPath filter over payload  [Jayway JsonPath]
      3. INSERT into consumer_events
```

Dedup key is `consumerId:eventId` — same event re-published within 24h is suppressed **per subscriber**.

### Consumer Polling API

Consumers poll `GET /api/v1/events?consumerId=&since=&limit=` and acknowledge with `POST /api/v1/events/{eventId}/ack`. Each response item contains: original `metadata`, `consumerMetadata` (consumerId + subscriptionVersion), `filteredPayload` (only subscribed fields), and `dataRefs` if present.

### Key Libraries

| Library | Purpose |
|---|---|
| `spring-kafka` | Kafka producer/consumer |
| `kafka-json-schema-serializer` (Confluent) | Schema registry integration |
| `com.jayway.jsonpath:json-path` | JSONPath field filtering |
| `spring-data-jpa` + `postgresql` | Subscription registry + consumer inbox |
| `spring-data-redis` | 24h dedup TTL |
| `flyway-core` | DB schema migrations |
| `org.testcontainers` | Integration tests (Kafka, Postgres, Redis) |

### Dead-Letter Queue (DLQ) + Retries

Two retry surfaces, both exposed in the UI:

1. **Server-side DLQ** (`dead_letter_events` table) — events that failed during Kafka consumer processing (bad JSON, schema error, filter crash, DB failure). Admin can retry (`POST /api/v1/admin/dlq/{id}/retry`) or discard.
2. **Unacked consumer events** — `consumer_events` rows where `read_at` is null past a configured threshold. Consumer or admin can reset `read_at` via `POST /api/v1/events/{id}/retry` so the event re-appears on the next poll.

`DeadLetterService.retry(id)` re-runs the full processing pipeline (dedup → filter → insert) for the original event snapshot stored in `original_event JSONB`.

### Next.js UI (`konductor-ui`)

```bash
# Development
cd konductor-ui
npm install
npm run dev        # http://localhost:3000

# Production build
npm run build
npm start
```

- **`NEXT_PUBLIC_API_URL`** env var points to Spring Boot (e.g. `http://localhost:8080`)
- Styling: **SCSS modules** only — no Tailwind. Global tokens in `src/styles/_variables.scss`
- API calls go through `src/lib/api.ts` (typed fetch wrapper) — never call `fetch` directly from components
- TypeScript types in `src/types/index.ts` mirror Java DTOs exactly

**UI pages:**

| Route | Purpose |
|---|---|
| `/events` | Consumer polling inbox — view filtered payload, ack events |
| `/retries` | Two tabs: Unacked events + DLQ events with retry/discard actions |
| `/subscriptions` | Consumer self-service — list own subscriptions, submit new requests |
| `/admin/subscriptions` | Admin approval queue — approve / reject pending subscriptions |

## Important Conventions

- **Never scatter `if (toggle)` checks** in business code — use `@ConditionalOnProperty` bean swaps.
- **Subscription `field_paths`** are stored as a JSONB array of JSONPath strings (e.g. `["$.orderId", "$.customer.email"]`). Evaluate them with `JsonPathFilterService`, not inline.
- **`output_topic`** column exists in `subscriptions` but delivery is currently polling-only; leave it populated but unused — it's reserved for a future Kafka-push delivery mode.
- Flyway migrations live in `konductor-server/src/main/resources/db/migration/` and follow `V{n}__{description}.sql` naming.
- Integration tests use Testcontainers — Docker must be running. They live in `src/test/java` alongside unit tests but are tagged with `@Tag("integration")`.
