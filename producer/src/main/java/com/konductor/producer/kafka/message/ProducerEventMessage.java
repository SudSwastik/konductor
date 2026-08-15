package com.konductor.producer.kafka.message;

import java.time.Instant;
import java.util.Map;

public record ProducerEventMessage(
        String sourceEventId,
        String triggerType,
        Instant occurredAt,
        String sourceSystem,
        String correlationId,
        Map<String, Object> body
) {
}
