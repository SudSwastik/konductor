package com.konductor.platform.subscription;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SubscriptionRepositoryTest {

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
    private SubscriptionRepository repository;

    @Test
    void findByEventTypeAndActiveTrue_returnsOnlyActiveSubscriptions() {
        repository.save(activeSubscription("order.created", "sub-a"));
        repository.save(activeSubscription("order.created", "sub-b"));
        repository.save(inactiveSubscription("order.created", "sub-c"));
        repository.save(activeSubscription("payment.processed", "sub-d"));

        List<SubscriptionEntity> results = repository.findByEventTypeAndActiveTrue("order.created");

        assertThat(results).hasSize(2)
                .extracting(SubscriptionEntity::getSubscriberId)
                .containsExactlyInAnyOrder("sub-a", "sub-b");
    }

    @Test
    void findByEventTypeAndActiveTrue_returnsEmpty_whenNoneActive() {
        repository.save(inactiveSubscription("order.created", "sub-x"));

        List<SubscriptionEntity> results = repository.findByEventTypeAndActiveTrue("order.created");

        assertThat(results).isEmpty();
    }

    @Test
    void save_persistsFieldPathsAsJsonb() {
        SubscriptionEntity saved = repository.save(activeSubscription("order.created", "sub-a"));

        SubscriptionEntity found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getFieldPaths()).containsExactly("$.orderId", "$.amount");
    }

    @Test
    void save_createdAtIsAutoPopulated() {
        SubscriptionEntity saved = repository.save(activeSubscription("order.created", "sub-a"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private SubscriptionEntity activeSubscription(String eventType, String subscriberId) {
        return SubscriptionEntity.builder()
                .subscriberId(subscriberId)
                .subscriberName(subscriberId + "-service")
                .eventType(eventType)
                .fieldPaths(List.of("$.orderId", "$.amount"))
                .outputTopic(subscriberId + "-events")
                .active(true)
                .build();
    }

    private SubscriptionEntity inactiveSubscription(String eventType, String subscriberId) {
        return SubscriptionEntity.builder()
                .subscriberId(subscriberId)
                .subscriberName(subscriberId + "-service")
                .eventType(eventType)
                .fieldPaths(List.of("$.orderId"))
                .outputTopic(subscriberId + "-events")
                .active(false)
                .build();
    }
}
