# Konductor — Documentation

## Table of Contents

1. [Architecture](#1-architecture)
2. [Schema Design](#2-schema-design)
3. [Flows](#3-flows)

---

# 1. Architecture

## Overview

Konductor is a **publisher-side event routing platform**. Producers publish events once; Konductor validates, fans them out through Kafka, applies per-subscription JSONPath filters, and delivers each subscriber's filtered slice to their own dedicated Kafka output topic.

Subscriber projects are fully separate — they import `konductor-model` as a Maven dependency and consume from their assigned topic however they choose.

---

## Module Layout

```
konductor/
├── konductor-model/        Shared event envelope POJOs (zero framework deps)
│                           Importable by any subscriber as a Maven dependency
│
├── konductor-producer/     Thin HTTP client library for data producers
│                           No Kafka / Redis / Spring Data deps
│                           Publishes via POST /api/v1/publish
│
├── konductor-platform/     Spring Boot 3.x platform
│                           ┌── INGEST ──────────────────────────────────────┐
│                           │  POST /api/v1/publish                          │
│                           │  Schema Registry validation (toggle)           │
│                           │  KafkaTemplate → internal topic                │
│                           ├── ROUTING ENGINE ──────────────────────────────┤
│                           │  @KafkaListener (internal topic)               │
│                           │  Load active subscriptions by eventType        │
│                           │  Redis dedup (24 h, subscriberId + eventId)    │
│                           │  JSONPath filter per subscription              │
│                           │  KafkaTemplate → subscription.output_topic     │
│                           │  DeadLetterService on failure                  │
│                           └── ADMIN API ───────────────────────────────────┘
│                              Subscription lifecycle management
│                              DLQ view / retry / discard
│
└── konductor-ui/           Next.js 14 admin UI
                            /admin/subscriptions  — register, approve, deactivate
                            /admin/dlq            — view, retry, discard
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Platform runtime | Java 17, Spring Boot 3.2.x |
| Build | Maven multi-module |
| Transport | Apache Kafka (KRaft, no Zookeeper) |
| Schema validation | Confluent Schema Registry (toggle) |
| Payload filtering | Jayway JsonPath |
| Subscription registry | PostgreSQL 16 + Spring Data JPA + Flyway |
| Deduplication | Redis (24 h TTL) |
| Large-payload refs | MinIO (S3-compatible) |
| Admin UI | Next.js 14, TypeScript, SCSS modules |
| Integration tests | Testcontainers |

---

## Component Diagram

```
                        ┌─────────────────────┐
                        │  Data Producer       │
                        │  (konductor-producer)│
                        └────────┬────────────┘
                                 │  POST /api/v1/publish
                                 ▼
                   ┌─────────────────────────────┐
                   │        PublishController     │
                   │        PublishService        │
                   │   SchemaRegistryValidator    │  ←── Confluent Schema Registry
                   │   KafkaTemplate.send(        │      (conditional toggle)
                   │     internal-topic, json)    │
                   └────────────┬────────────────┘
                                │  Kafka: konductor-events
                                ▼
                   ┌─────────────────────────────┐
                   │  EventRouter                 │
                   │  @KafkaListener              │
                   │        │                     │
                   │        ▼                     │
                   │  EventProcessingService      │
                   │    for each subscription:    │
                   │      DeduplicationService ──────► Redis
                   │      JsonPathFilterService   │
                   │      KafkaTemplate.send(     │
                   │        output_topic, event)  │
                   │      on error:               │
                   │        DeadLetterService ──────► PostgreSQL
                   └─────────────────────────────┘
                                │
                       per-subscriber Kafka topics
                                │
               ┌────────────────┴────────────────┐
               ▼                                 ▼
      Subscriber A topic                Subscriber B topic
      (separate project)               (separate project)
      imports konductor-model          imports konductor-model
```

---

## Infrastructure Services

| Service | Port | Purpose |
|---|---|---|
| Kafka (KRaft) | 9092 | Internal event transport |
| Confluent Schema Registry | 8081 | Optional schema validation |
| PostgreSQL 16 | 5432 | Subscription registry + DLQ |
| Redis 7 | 6379 | 24 h deduplication TTL |
| MinIO | 9000 / 9001 | S3-compatible large-payload refs |
| konductor-platform | 8080 | Spring Boot REST API |
| konductor-ui | 3000 | Next.js admin UI |

Start everything with:
```bash
docker compose up -d
```

---

## Key Design Decisions

| Decision | Choice | Why |
|---|---|---|
| Subscriber delivery | Kafka `output_topic` per subscription | Subscribers are independent; each picks their own consumption pattern |
| No consumer code in this repo | Separate subscriber projects | Many subscriber types; forcing one pattern would be wrong |
| `konductor-model` as shared contract | Published as Maven dep | Subscriber projects import only the POJOs, no infrastructure |
| Schema toggle scope | Global (`application.yml`) | Simple; per-eventType toggle adds complexity without immediate need |
| Dedup key | `subscriberId:eventId` | Scoped per subscriber, 24 h TTL — same event won't re-route to same topic |
| Filtered event shape | Same `KonductorEvent` envelope, filtered payload | Subscriber gets consistent envelope; only payload fields differ |
| DLQ storage | PostgreSQL `dead_letter_events` | Consistent with data layer; `original_event JSONB` enables replay |

---

# 2. Schema Design

## Event Envelope (`konductor-model`)

Every event flowing through Konductor uses the `KonductorEvent` envelope. It has three fixed sections:

```
KonductorEvent
├── metadata   (EventMetadata)        — fixed, platform-controlled
├── payload    (Map<String, Object>)  — dynamic JSON, producer-defined
└── dataRefs   (List<DataRef>)        — nullable, for large external payloads
```

### EventMetadata

| Field | Type | Notes |
|---|---|---|
| `eventId` | `UUID` | Auto-generated by `KonductorEventBuilder` |
| `eventType` | `String` | Required — used for subscription matching (e.g. `order.created`) |
| `eventTimestamp` | `Instant` | Auto-set to `Instant.now()` by builder |
| `sourceConfigId` | `String` | Required — identifies the producing service |
| `configType` | `String` | Optional — producer-defined classification |
| `schemaVersion` | `String` | Optional — schema version hint |
| `correlationId` | `String` | Optional — for distributed tracing |
| `traceId` | `String` | Optional — for distributed tracing |

### DataRef (large-payload pointer)

| Field | Type | Notes |
|---|---|---|
| `uri` | `String` | MinIO / S3 URL |
| `contentType` | `String` | e.g. `application/octet-stream` |
| `checksum` | `String` | SHA-256 hex |
| `sizeBytes` | `Long` | File size in bytes |

### Example — Full KonductorEvent JSON

```json
{
  "metadata": {
    "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "eventType": "order.created",
    "eventTimestamp": "2026-06-25T10:00:00Z",
    "sourceConfigId": "order-service",
    "configType": "commerce",
    "schemaVersion": "1.0",
    "correlationId": "corr-xyz",
    "traceId": "trace-abc"
  },
  "payload": {
    "orderId": "ORD-001",
    "amount": 149.99,
    "currency": "USD",
    "customer": {
      "id": "cust-42",
      "email": "alice@example.com",
      "tier": "gold"
    },
    "items": [
      { "sku": "WIDGET-A", "qty": 2, "price": 49.99 },
      { "sku": "WIDGET-B", "qty": 1, "price": 50.01 }
    ]
  },
  "dataRefs": null
}
```

---

## PostgreSQL Schema

### `subscriptions`

Stores admin-registered subscriptions. All start `active = false` and require explicit approval.

```sql
CREATE TABLE subscriptions (
    id                   UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    subscriber_id        VARCHAR NOT NULL,          -- e.g. "analytics-service"
    subscriber_name      VARCHAR NOT NULL,          -- human-readable display name
    subscription_version INT     NOT NULL DEFAULT 1, -- bumped on each approval
    event_type           VARCHAR NOT NULL,          -- matches KonductorEvent.metadata.eventType
    field_paths          JSONB   NOT NULL,          -- ["$.orderId", "$.amount"]
    output_topic         VARCHAR NOT NULL,          -- Kafka topic to route filtered events to
    active               BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMP WITH TIME ZONE,
    approved_at          TIMESTAMP WITH TIME ZONE
);
```

**Lifecycle states** derived from columns:

| State | Condition |
|---|---|
| PENDING | `active = false`, `approved_at IS NULL` |
| APPROVED | `active = true` |
| DEACTIVATED | `active = false`, `approved_at IS NOT NULL` |

### `dead_letter_events`

Events that failed routing are parked here for review and replay.

```sql
CREATE TABLE dead_letter_events (
    id             UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       UUID,                           -- from KonductorEvent.metadata.eventId
    event_type     VARCHAR,                        -- from KonductorEvent.metadata.eventType
    original_event JSONB,                          -- full KonductorEvent snapshot for replay
    subscriber_id  VARCHAR,                        -- which subscriber failed (null if pre-routing)
    failure_reason TEXT,                           -- exception message
    retry_count    INT     NOT NULL DEFAULT 0,
    last_retry_at  TIMESTAMP WITH TIME ZONE,
    status         VARCHAR NOT NULL DEFAULT 'PENDING', -- PENDING | RETRIED | DISCARDED
    created_at     TIMESTAMP WITH TIME ZONE
);
```

**Status values:**

| Status | Meaning |
|---|---|
| `PENDING` | Failed, awaiting admin action |
| `RETRIED` | Re-processed via admin retry endpoint |
| `DISCARDED` | Explicitly abandoned — no further processing |

---

## Redis Key Schema

| Key pattern | Type | TTL | Purpose |
|---|---|---|---|
| `dedup:{subscriberId}:{eventId}` | String (`"1"`) | 24 h | Prevent duplicate routing to same subscriber |

Example: `dedup:analytics-service:a1b2c3d4-e5f6-7890-abcd-ef1234567890`

---

## Filtered Event

When routing to a subscriber, Konductor sends a full `KonductorEvent` with the **same** `metadata` and `dataRefs`, but `payload` reduced to only the subscriber's subscribed `field_paths`.

**Original payload:**
```json
{ "orderId": "ORD-001", "amount": 149.99, "customerEmail": "alice@example.com" }
```

**Subscription `field_paths`:** `["$.orderId", "$.amount"]`

**Filtered payload sent to subscriber's topic:**
```json
{ "orderId": "ORD-001", "amount": 149.99 }
```

---

# 3. Flows

## 3.1 Event Publishing Flow

```
Producer (using konductor-producer SDK)
  │
  │  KonductorEventBuilder.newEvent()
  │    .eventType("order.created")
  │    .sourceConfigId("order-service")
  │    .payload(Map.of("orderId", "ORD-001", "amount", 149.99))
  │    .build()
  │
  │  publisher.publish(event)  →  POST /api/v1/publish
  │
  ▼
PublishController.publish(KonductorEvent)
  │
  ├─ [schema-registry.enabled=true]
  │    ConfluentSchemaRegistryValidator.validate(event)
  │       GET /subjects/{eventType}-value/versions/latest
  │       → 404 / error  →  HTTP 400 Bad Request (no schema registered)
  │       → 200 OK       →  continue
  │
  ├─ [schema-registry.enabled=false]
  │    NoOpSchemaRegistryValidator.validate(event)  →  pass-through
  │
  ├─ ObjectMapper.writeValueAsString(event)
  │
  ├─ KafkaTemplate.send("konductor-events", eventId, json)
  │
  └─ HTTP 202 Accepted  →  { "eventId": "a1b2c3d4-..." }
```

---

## 3.2 Routing Flow

```
Kafka topic: konductor-events
  │
  ▼
EventRouter.onEvent(KonductorEvent)   ← @KafkaListener
  │
  ▼
EventProcessingService.processEvent(event)
  │
  ├─ subscriptionRepository.findByEventTypeAndActiveTrue(eventType)
  │    → List<SubscriptionEntity>   (may be empty — no-op)
  │
  └─ for each SubscriptionEntity:
       │
       ├─ DeduplicationService.isDuplicate(subscriberId, eventId)
       │    Redis SETNX "dedup:{subscriberId}:{eventId}" TTL 24h
       │    → true   →  SKIP (already processed)
       │    → false  →  continue
       │
       ├─ JsonPathFilterService.filter(payload, fieldPaths)
       │    Evaluates each JSONPath against payload
       │    Builds filtered Map<String, Object>
       │    Missing paths are silently skipped
       │
       ├─ Build filtered KonductorEvent
       │    same metadata + dataRefs
       │    reduced payload
       │
       ├─ KafkaTemplate.send(subscription.outputTopic, eventId, filteredJson)
       │
       └─ on ANY exception:
            DeadLetterService.park(event, subscriberId, errorMessage)
            → INSERT INTO dead_letter_events (status='PENDING')
```

---

## 3.3 Deduplication Flow

```
isDuplicate(subscriberId, eventId):
  │
  ├─ key = "dedup:{subscriberId}:{eventId}"
  │
  ├─ Redis SETNX key "1" TTL=24h
  │    → key was absent → SET succeeds → returns true (isNew=true)
  │                                    → isDuplicate = false  ← first time seen
  │    → key existed   → SET fails    → returns false (isNew=false)
  │                                    → isDuplicate = true   ← duplicate
  │
  └─ Same event re-published within 24h window:
       Sub A already processed → skipped
       Sub B not yet processed → allowed (keys are scoped per subscriber)
```

---

## 3.4 DLQ Retry Flow

```
Admin: POST /api/v1/admin/dlq/{id}/retry
  │
  ▼
AdminDlqController.retry(id)
  │
  ▼
DeadLetterService.retry(id)
  │
  ├─ SELECT * FROM dead_letter_events WHERE id = ?
  │
  ├─ ObjectMapper.readValue(entity.originalEvent, KonductorEvent.class)
  │    Deserialises the full event snapshot stored at failure time
  │
  ├─ EventProcessingService.processEvent(event)
  │    Re-runs the full routing pipeline (dedup + filter + send)
  │    Note: if eventId TTL has not expired in Redis, dedup will SKIP again
  │
  ├─ entity.retryCount++
  ├─ entity.lastRetryAt = now()
  ├─ entity.status = "RETRIED"
  └─ UPDATE dead_letter_events
```

---

## 3.5 Subscription Lifecycle

```
Admin: POST /api/v1/admin/subscriptions
  │  { subscriberId, subscriberName, eventType, fieldPaths, outputTopic }
  │
  └─ INSERT INTO subscriptions (active=false)   →  status: PENDING


Admin: PUT /api/v1/admin/subscriptions/{id}/approve
  │
  └─ active = true
     approved_at = now()
     subscription_version++                     →  status: APPROVED
     (EventRouter now routes eventType to this subscriber)


Admin: PUT /api/v1/admin/subscriptions/{id}/reject
  │
  └─ active = false (no change if already false) →  status: PENDING


Admin: DELETE /api/v1/admin/subscriptions/{id}
  │
  └─ active = false                              →  status: DEACTIVATED
     (EventRouter stops routing to this subscriber)
```

---

## 3.6 Schema Registry Toggle

Controlled by a single property — no if/else in business code:

```yaml
# application.yml
konductor:
  schema-registry:
    enabled: false   # true = ConfluentSchemaRegistryValidator
                     # false (default) = NoOpSchemaRegistryValidator
```

Bean swap via `@ConditionalOnProperty`:

```
enabled=true  → ConfluentSchemaRegistryValidator
                 GET {url}/subjects/{eventType}-value/versions/latest
                 404 → throw ResponseStatusException(400)

enabled=false → NoOpSchemaRegistryValidator
                 void validate() { } // pass-through
```

---

## 3.7 JSONPath Filtering

Given subscription `field_paths = ["$.orderId", "$.amount"]`:

```
payload:
{
  "orderId": "ORD-001",
  "amount": 149.99,
  "customerEmail": "alice@example.com",
  "customer": { "name": "Alice", "tier": "gold" }
}

JSONPath evaluation:
  "$.orderId"  → key: "orderId",  value: "ORD-001"
  "$.amount"   → key: "amount",   value: 149.99

filtered payload:
{
  "orderId": "ORD-001",
  "amount": 149.99
}
```

Rules:
- `$.fieldName` → top-level field, key = `fieldName`
- `$.a.b` → nested field, key = `a.b` (flat map entry)
- Path not found in payload → silently skipped (no error)
- Empty / null `field_paths` → full payload passed through
