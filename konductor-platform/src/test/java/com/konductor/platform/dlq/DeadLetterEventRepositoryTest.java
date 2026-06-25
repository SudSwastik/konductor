package com.konductor.platform.dlq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DeadLetterEventRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("konductor")
            .withUsername("konductor")
            .withPassword("konductor");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private DeadLetterEventRepository repository;

    @Test
    void findByStatusOrderByCreatedAtDesc_returnsOnlyMatchingStatus() {
        repository.save(dlqEvent("PENDING", "order.created"));
        repository.save(dlqEvent("PENDING", "payment.processed"));
        repository.save(dlqEvent("DISCARDED", "order.created"));

        List<DeadLetterEventEntity> results = repository.findByStatusOrderByCreatedAtDesc("PENDING");

        assertThat(results).hasSize(2)
                .allMatch(e -> e.getStatus().equals("PENDING"));
    }

    @Test
    void findByStatusOrderByCreatedAtDesc_returnsEmpty_whenNoneMatch() {
        repository.save(dlqEvent("DISCARDED", "order.created"));

        List<DeadLetterEventEntity> results = repository.findByStatusOrderByCreatedAtDesc("PENDING");

        assertThat(results).isEmpty();
    }

    @Test
    void save_defaultsRetryCountToZero() {
        DeadLetterEventEntity saved = repository.save(dlqEvent("PENDING", "order.created"));

        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void save_createdAtIsAutoPopulated() {
        DeadLetterEventEntity saved = repository.save(dlqEvent("PENDING", "order.created"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private DeadLetterEventEntity dlqEvent(String status, String eventType) {
        return DeadLetterEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .originalEvent("{\"metadata\":{\"eventType\":\"" + eventType + "\"}}")
                .subscriberId("sub-a")
                .failureReason("Simulated failure")
                .status(status)
                .build();
    }
}
