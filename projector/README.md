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
V1__subscription_type.sql
V2__subscription_status.sql
V3__event_trigger_type.sql
V4__event_status.sql
V5__parameter_type.sql
V6__subscription.sql
V7__event_trigger_selection.sql
V8__parameter_definition.sql
V9__parameter_selection.sql
V10__delivery_config.sql
V11__event.sql
V12__event_retry_log.sql
V13__subscription_audit_log.sql
V14__create_indexes.sql
V15__seed_master_data.sql
```

Current schema tables:

```text
subscription_type
subscription_status
event_trigger_type
event_status
parameter_type
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
SPRING_DATASOURCE_USERNAME=konductor_app
SPRING_DATASOURCE_PASSWORD=change-me
```

## Build And Test

```bash
mvn clean verify
```
