package com.konductor.platform.routing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPathFilterServiceTest {

    private final JsonPathFilterService service = new JsonPathFilterService();

    @Test
    void filter_returnsOnlyRequestedFields() {
        Map<String, Object> payload = Map.of(
                "orderId", "abc-123",
                "amount", 99.5,
                "customerEmail", "user@example.com");

        Map<String, Object> result = service.filter(payload, List.of("$.orderId", "$.amount"));

        assertThat(result).containsOnlyKeys("orderId", "amount")
                .containsEntry("orderId", "abc-123")
                .containsEntry("amount", 99.5);
    }

    @Test
    void filter_ignoresMissingPaths() {
        Map<String, Object> payload = Map.of("orderId", "abc-123");

        Map<String, Object> result = service.filter(payload, List.of("$.orderId", "$.nonexistent"));

        assertThat(result).containsOnlyKeys("orderId");
    }

    @Test
    void filter_returnsFullPayload_whenPathsEmpty() {
        Map<String, Object> payload = Map.of("orderId", "abc-123", "amount", 99.5);

        Map<String, Object> result = service.filter(payload, List.of());

        assertThat(result).isEqualTo(payload);
    }

    @Test
    void filter_returnsFullPayload_whenPathsNull() {
        Map<String, Object> payload = Map.of("orderId", "abc-123");

        Map<String, Object> result = service.filter(payload, null);

        assertThat(result).isEqualTo(payload);
    }

    @Test
    void filter_handlesNestedPath() {
        Map<String, Object> payload = Map.of(
                "orderId", "abc-123",
                "customer", Map.of("name", "Alice", "tier", "gold"));

        Map<String, Object> result = service.filter(payload, List.of("$.customer.name"));

        assertThat(result).containsEntry("customer.name", "Alice");
    }

    @Test
    void filter_returnsEmptyMap_whenNoPathsMatch() {
        Map<String, Object> payload = Map.of("orderId", "abc-123");

        Map<String, Object> result = service.filter(payload, List.of("$.missing1", "$.missing2"));

        assertThat(result).isEmpty();
    }
}
