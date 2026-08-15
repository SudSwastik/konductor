package com.konductor.producer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.Map;

public record PublishEventRequest(
        @NotBlank String triggerType,
        String sourceEventId,
        Instant occurredAt,
        String sourceSystem,
        String correlationId,
        @NotEmpty
        Map<String, Object> body
) {
}
