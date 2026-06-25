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
mvn test -pl konductor-producer
mvn test -pl konductor-platform

# Run a single test class
mvn test -pl konductor-platform -Dtest=JsonPathFilterServiceTest

# Run integration tests (requires Docker for Testcontainers)
mvn verify -pl konductor-platform

# UI (Next.js)
cd konductor-ui && npm run dev    # dev server on :3000
cd konductor-ui && npm run build  # production build
```

## What This Project Is (and Is Not)

**Konductor is a publisher-side event routing platform.** It accepts events from producers, validates them, routes them through Kafka, applies per-subscription JSONPath filters, and delivers each subscriber's filtered slice to their own Kafka output topic.

**There is no consumer/subscriber code in this project.** Subscriber projects are separate services that import `konductor-model` for the event envelope types and consume from their own output topic however they choose.

## Module Layout

```
konductor/
├── konductor-model/   # Shared envelope POJOs — importable by any subscriber project
├── konductor-producer/  # Thin HTTP library for data producers (no Kafka dep)
├── konductor-platform/  # Spring Boot platform — ingest, routing, admin API
└── konductor-ui/      # Next.js 14 admin UI (subscriptions + DLQ only)
```

## Architecture

### Event Envelope (3 parts — defined in `konductor-model`)

Every `KonductorEvent` has exactly three sections:

1. **`metadata`** (`EventMetadata`) — fixed schema. Fields: `eventId` (UUID), `eventType`, `eventTimestamp`, `sourceConfigId`, `configType`, `schemaVersion`, `correlationId`, `traceId`.
2. **`payload`** (`Map<String, Object>`) — dynamic JSON. Validated against Confluent Schema Registry when toggle is ON.
3. **`dataRefs`** (`List<DataRef>`, nullable) — external large-payload pointers. Each: `uri`, `contentType`, `checksum` (SHA-256), `sizeBytes`.

### Routing Flow

```
Producer → konductor-producer → POST /api/v1/publish
                                      │
                               [schema registry?]
                                      │
                              Kafka (internal topic)
                                      │
                          @KafkaListener (EventRouter)
                                      │
                     for each active subscription (by eventType):
                              │
                        dedup check (Redis 24h)    → skip if duplicate
                              │
                        JSONPath filter             → filtered KonductorEvent
                              │
                        KafkaProducer → subscription.output_topic
                              │
                 (on failure) → dead_letter_events (PostgreSQL)
```

The filtered event sent to `output_topic` is a full `KonductorEvent` with the same `metadata` and `dataRefs` but only the subscriber's subscribed `payload` fields.

### Schema Registry Toggle

```yaml
konductor:
  schema-registry:
    enabled: true   # false = skip validation entirely
    url: http://schema-registry:8081
  kafka:
    internal-topic: konductor-events
```

Wired via `@ConditionalOnProperty` bean swap — `SchemaRegistryValidator` is a no-op bean when `enabled=false`. No if/else in business code.

### Subscription Registry (PostgreSQL)

Admin registers subscriptions specifying `subscriberId`, `eventType`, `field_paths` (JSONPath array), and `output_topic`. Subscriptions start `active=false` and require explicit admin approval.

Tables:
- `subscriptions` — subscriberId, eventType, field_paths (JSONB), output_topic, active, subscription_version
- `dead_letter_events` — failed routing events; `original_event JSONB` stores full snapshot for replay

### konductor-producer

Producers embed this library. It has zero infrastructure dependencies — just HTTP:
```java
KonductorEvent event = KonductorEventBuilder.newEvent()
    .eventType("order.created")
    .sourceConfigId("order-service")
    .payload(Map.of("orderId", "abc", "amount", 99.5))
    .build();

publisher.publish(event);  // POST /api/v1/publish
```
Config: `konductor.client.server-url=http://konductor-platform:8080`

### konductor-model (for subscriber projects)

Subscriber projects add this as a Maven dependency to get `KonductorEvent`, `EventMetadata`, `DataRef`. They then consume from their designated `output_topic` and deserialise using standard Jackson. No other Konductor dependency needed.

### DLQ

Events that fail routing (JSONPath error, Kafka send failure, etc.) land in `dead_letter_events` with the full original event snapshot. Admin can retry (`POST /api/v1/admin/dlq/{id}/retry`) which re-runs the routing pipeline, or discard.

### Admin UI (`konductor-ui`)

Admin-only — no consumer views.

| Route | Purpose |
|---|---|
| `/admin/subscriptions` | Register, approve, reject, deactivate subscriptions |
| `/admin/dlq` | View failed events, retry or discard |

- Styling: **SCSS modules only** — global tokens in `src/styles/_variables.scss`
- API calls go through `src/lib/api.ts` — never call `fetch` directly from components

## Important Conventions

- **No subscriber/consumer code belongs in this repo.** If you find yourself writing polling endpoints or consumer-side logic, stop — that lives in a separate project.
- **`konductor-producer` has zero infrastructure deps** — no Kafka, no Redis, no Spring Data.
- **Schema registry toggle** uses `@ConditionalOnProperty` bean swap, not if/else checks scattered in code.
- **`field_paths`** are a JSONB array of JSONPath strings. Always evaluate via `JsonPathFilterService`.
- **`output_topic`** is mandatory on every subscription — it's where filtered events are routed.
- **Filtered events** use the same `KonductorEvent` envelope; only `payload` is reduced to subscribed fields.
- Flyway migrations: `konductor-platform/src/main/resources/db/migration/V{n}__{description}.sql`
- Integration tests require Docker (Testcontainers). Tag them `@Tag("integration")`.

### Key Libraries

| Library | Module | Purpose |
|---|---|---|
| `spring-kafka` | platform | Kafka producer (ingest → internal topic) + EventRouter (@KafkaListener) |
| `kafka-json-schema-serializer` (Confluent) | platform | Schema registry integration |
| `com.jayway.jsonpath:json-path` | platform | JSONPath payload filtering |
| `spring-data-jpa` + `postgresql` | platform | Subscription registry + DLQ |
| `spring-data-redis` | platform | 24h dedup TTL |
| `flyway-core` | platform | DB migrations |
| `com.networknt:json-schema-validator` | platform | Payload validation against registry schema |
| `org.testcontainers` | server (test) | Kafka, Postgres, Redis in integration tests |
