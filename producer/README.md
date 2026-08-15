# Producer

Producer emits sample source events to Kafka for projector testing.

## Run

```bash
mvn spring-boot:run
```

The service starts on port `8084`.

```bash
curl http://localhost:8084/actuator/health
```

Producer publishes to `konductor.source-events` by default.

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
KONDUCTOR_SOURCE_EVENTS_TOPIC=konductor.source-events
```

## Publish Events

```bash
curl -X POST http://localhost:8084/api/v1/events/ORDER_CREATED
curl -X POST http://localhost:8084/api/v1/events/PAYMENT_UPDATED
curl -X POST http://localhost:8084/api/v1/events/SHIPMENT_UPDATED
```
