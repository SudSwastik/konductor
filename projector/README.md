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

## Build And Test

```bash
mvn clean verify
```
