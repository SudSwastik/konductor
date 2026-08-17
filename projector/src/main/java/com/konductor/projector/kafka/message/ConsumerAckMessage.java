package com.konductor.projector.kafka.message;

import java.time.Instant;

public record ConsumerAckMessage(
        String eventUid,
        String consumerName,
        String status,
        String message,
        Instant acknowledgedAt
) {
}
