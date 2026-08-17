package com.konductor.projector.kafka.message;

import java.time.Instant;
import java.util.Map;

public record ProjectedEventMessage(
        String eventUid,
        String sourceEventId,
        String subscriptionUid,
        String triggerType,
        Instant projectedAt,
        Map<String, Object> data
) {
}
