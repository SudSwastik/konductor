package com.konductor.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.model.KonductorEvent;
import com.konductor.model.KonductorEventBuilder;
import com.konductor.platform.dlq.DeadLetterEventEntity;
import com.konductor.platform.dlq.DeadLetterEventRepository;
import com.konductor.platform.subscription.SubscriptionRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class KonductorIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("konductor")
            .withUsername("konductor")
            .withPassword("konductor");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    DeadLetterEventRepository deadLetterEventRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("konductor.schema-registry.enabled", () -> "false");
        registry.add("konductor.kafka.internal-topic", () -> "konductor-events");
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    void publishedEvent_routedToSubscriber_withFilteredPayload() throws Exception {
        String outputTopic = "it-output-" + UUID.randomUUID();
        registerAndApprove("it-sub-filter", "order.created", List.of("$.orderId", "$.amount"), outputTopic);

        KonductorEvent event = KonductorEventBuilder.newEvent()
                .eventType("order.created")
                .sourceConfigId("it-producer")
                .payload(Map.of("orderId", "ORD-IT-001", "amount", 149.99, "secret", "hidden-value"))
                .build();

        ResponseEntity<Map> publishResponse = restTemplate.postForEntity("/api/v1/publish", event, Map.class);

        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(publishResponse.getBody()).containsKey("eventId");

        Optional<ConsumerRecord<String, String>> record = pollForRecord(outputTopic, 20);
        assertThat(record).as("filtered event should arrive on output topic").isPresent();

        JsonNode filteredEvent = objectMapper.readTree(record.get().value());
        JsonNode payload = filteredEvent.get("payload");
        assertThat(payload.has("orderId")).isTrue();
        assertThat(payload.has("amount")).isTrue();
        assertThat(payload.has("secret")).as("secret field should be filtered out").isFalse();
        assertThat(filteredEvent.get("metadata").get("eventType").asText()).isEqualTo("order.created");
    }

    @Test
    void duplicateEvent_notRoutedAgain_toSameSubscriber() throws Exception {
        String outputTopic = "it-output-dedup-" + UUID.randomUUID();
        registerAndApprove("it-sub-dedup", "payment.processed", List.of("$.amount"), outputTopic);

        KonductorEvent event = KonductorEventBuilder.newEvent()
                .eventType("payment.processed")
                .sourceConfigId("it-producer")
                .payload(Map.of("amount", 49.99))
                .build();

        // First publish — should be routed
        restTemplate.postForEntity("/api/v1/publish", event, Map.class);
        Optional<ConsumerRecord<String, String>> first = pollForRecord(outputTopic, 20);
        assertThat(first).as("first event should be routed").isPresent();

        // Second publish with same eventId — should be deduped
        restTemplate.postForEntity("/api/v1/publish", event, Map.class);
        Optional<ConsumerRecord<String, String>> second = pollForRecord(outputTopic, 5);
        assertThat(second).as("duplicate event should not be re-routed").isEmpty();
    }

    @Test
    void subscriptionLifecycle_register_approve_deactivate() {
        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/admin/subscriptions",
                Map.of(
                        "subscriberId", "it-lifecycle-sub",
                        "subscriberName", "Lifecycle Service",
                        "eventType", "lifecycle.test",
                        "fieldPaths", List.of("$.id"),
                        "outputTopic", "it-lifecycle-events"),
                Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("active")).isEqualTo(false);

        String id = created.getBody().get("id").toString();

        // Approve
        ResponseEntity<Map> approved = restTemplate.exchange(
                "/api/v1/admin/subscriptions/" + id + "/approve",
                HttpMethod.PUT, null, Map.class);
        assertThat(approved.getBody().get("active")).isEqualTo(true);
        assertThat(approved.getBody().get("approvedAt")).isNotNull();
        assertThat(approved.getBody().get("subscriptionVersion")).isEqualTo(2);

        // Deactivate
        restTemplate.delete("/api/v1/admin/subscriptions/" + id);
        assertThat(subscriptionRepository.findById(UUID.fromString(id))
                .map(e -> e.isActive()).orElse(true)).isFalse();
    }

    @Test
    void dlqEvent_canBeListedAndDiscarded_viaAdminApi() {
        DeadLetterEventEntity seeded = deadLetterEventRepository.save(
                DeadLetterEventEntity.builder()
                        .eventId(UUID.randomUUID())
                        .eventType("it.dlq.test")
                        .originalEvent("{\"metadata\":{\"eventType\":\"it.dlq.test\"}}")
                        .subscriberId("it-dlq-sub")
                        .failureReason("Seeded by integration test")
                        .build());

        // List — should appear
        ResponseEntity<List> listResponse = restTemplate.getForEntity(
                "/api/v1/admin/dlq?status=PENDING", List.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotEmpty();

        // Discard
        restTemplate.postForEntity(
                "/api/v1/admin/dlq/" + seeded.getId() + "/discard", null, Void.class);

        assertThat(deadLetterEventRepository.findById(seeded.getId())
                .map(DeadLetterEventEntity::getStatus).orElse(""))
                .isEqualTo("DISCARDED");
    }

    @Test
    void dlqEvent_retry_reroutesOriginalEvent() throws Exception {
        String outputTopic = "it-output-retry-" + UUID.randomUUID();
        registerAndApprove("it-sub-retry", "retry.event", List.of("$.value"), outputTopic);

        // Build a known event to use as the stored original
        KonductorEvent original = KonductorEventBuilder.newEvent()
                .eventType("retry.event")
                .sourceConfigId("it-producer")
                .payload(Map.of("value", "retried-payload"))
                .build();

        String originalJson = objectMapper.writeValueAsString(original);

        DeadLetterEventEntity seeded = deadLetterEventRepository.save(
                DeadLetterEventEntity.builder()
                        .eventId(original.getMetadata().getEventId())
                        .eventType("retry.event")
                        .originalEvent(originalJson)
                        .subscriberId("it-sub-retry")
                        .failureReason("Seeded for retry test")
                        .build());

        restTemplate.postForEntity(
                "/api/v1/admin/dlq/" + seeded.getId() + "/retry", null, Void.class);

        // DLQ entry should be marked RETRIED
        assertThat(deadLetterEventRepository.findById(seeded.getId())
                .map(DeadLetterEventEntity::getStatus).orElse(""))
                .isEqualTo("RETRIED");

        // Routed event should appear on output topic
        Optional<ConsumerRecord<String, String>> record = pollForRecord(outputTopic, 20);
        assertThat(record).as("retried event should be routed to output topic").isPresent();

        JsonNode payload = objectMapper.readTree(record.get().value()).get("payload");
        assertThat(payload.get("value").asText()).isEqualTo("retried-payload");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void registerAndApprove(String subscriberId, String eventType,
                                     List<String> fieldPaths, String outputTopic) {
        ResponseEntity<Map> created = restTemplate.postForEntity(
                "/api/v1/admin/subscriptions",
                Map.of(
                        "subscriberId", subscriberId,
                        "subscriberName", subscriberId + "-service",
                        "eventType", eventType,
                        "fieldPaths", fieldPaths,
                        "outputTopic", outputTopic),
                Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String id = created.getBody().get("id").toString();
        restTemplate.exchange("/api/v1/admin/subscriptions/" + id + "/approve",
                HttpMethod.PUT, null, Map.class);
    }

    private Optional<ConsumerRecord<String, String>> pollForRecord(String topic, int timeoutSeconds) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    return Optional.of(records.iterator().next());
                }
            }
        }
        return Optional.empty();
    }
}
