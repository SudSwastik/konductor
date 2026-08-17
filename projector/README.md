# Projector

Projector is the Spring Boot service that will receive source events, apply subscriber field selections, and send projected payloads downstream.

## Run

```bash
mvn spring-boot:run
```

The service starts on port `8082`.

```bash
curl http://localhost:8082/actuator/health
```

## Database

Projector uses PostgreSQL. Schema changes live in Flyway migrations under `src/main/resources/db/migration`.

Current migration files:

```text
V1__create_tables.sql
V2__seed_master_data.sql
V3__seed_parameter_definitions.sql
```

Current schema tables:

```text
subscription_type
subscription_status
event_trigger_type
event_status
parameter_data_type
subscription
event_trigger_selection
parameter_definition
parameter_selection
delivery_config
event
event_retry_log
subscription_audit_log
```

All tables use an internal numeric `id` for primary keys and foreign keys. The `subscription` table has `subscription_uid` and the `event` table has `event_uid` for external/API references.

Provide database connection values with environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/konductor
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
KONDUCTOR_SOURCE_EVENTS_TOPIC=konductor.source-events
KONDUCTOR_SUBSCRIPTION_TOPIC_PREFIX=konductor.subscription
KONDUCTOR_CONSUMER_ACKS_TOPIC=konductor.consumer-acks
```

## Kafka Flow

Projector consumes source events from `konductor.source-events`.

For every matching active subscription, projector creates an `event` row, projects only the selected parameters, and publishes the projected payload to one subscription-specific topic:

```text
konductor.subscription.<subscription_uid>
```

If the subscription has an active `delivery_config.endpoint_url`, projector uses that value as the Kafka topic instead.

Consumers publish acknowledgements to the single shared ACK topic:

```text
konductor.consumer-acks
```

Projector marks the event as `DELIVERED` for `ACKED` acknowledgements and `DELIVERY_FAILED` for failed acknowledgements.

## Build And Test

```bash
mvn clean verify
```
