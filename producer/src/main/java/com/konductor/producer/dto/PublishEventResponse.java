package com.konductor.producer.dto;

import java.time.Instant;

public record PublishEventResponse(
        String topic,
        String sourceEventId,
        String triggerType,
        Instant occurredAt
) {
}
