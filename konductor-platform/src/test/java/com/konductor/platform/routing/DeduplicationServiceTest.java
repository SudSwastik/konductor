package com.konductor.platform.routing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DeduplicationServiceTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static DeduplicationService service;

    @BeforeAll
    static void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();

        service = new DeduplicationService(template);
    }

    @Test
    void isDuplicate_returnsFalse_firstSeen() {
        assertThat(service.isDuplicate("sub-a", UUID.randomUUID())).isFalse();
    }

    @Test
    void isDuplicate_returnsTrue_secondCall_sameSubscriberAndEvent() {
        UUID eventId = UUID.randomUUID();
        service.isDuplicate("sub-b", eventId);
        assertThat(service.isDuplicate("sub-b", eventId)).isTrue();
    }

    @Test
    void isDuplicate_returnsFalse_sameEvent_differentSubscriber() {
        UUID eventId = UUID.randomUUID();
        service.isDuplicate("sub-c", eventId);
        assertThat(service.isDuplicate("sub-d", eventId)).isFalse();
    }

    @Test
    void isDuplicate_returnsFalse_differentEvent_sameSubscriber() {
        service.isDuplicate("sub-e", UUID.randomUUID());
        assertThat(service.isDuplicate("sub-e", UUID.randomUUID())).isFalse();
    }
}
