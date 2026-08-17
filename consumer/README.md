# Consumer

Consumer is a sample downstream Spring Boot service. It listens to one subscription-specific Kafka topic and publishes acknowledgements to the shared consumer ACK topic.

## Run

```bash
mvn spring-boot:run
```

The service starts on port `8086`.

```bash
curl http://localhost:8086/actuator/health
```

## Kafka

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
KONDUCTOR_SUBSCRIPTION_TOPIC=konductor.subscription.<subscription_uid>
KONDUCTOR_CONSUMER_ACKS_TOPIC=konductor.consumer-acks
KONDUCTOR_CONSUMER_NAME=sample-consumer
```

Projector publishes to `konductor.subscription.<subscription_uid>` by default. If a subscription has a delivery config with `endpointUrl`, projector uses that value as the topic instead.
