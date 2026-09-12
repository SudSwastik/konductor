package com.konductor.projector.dto.response;

import java.time.Instant;

public record EventResponse(
        String eventUid,
        String sourceEventId,
        String subscriptionUid,
        String triggerType,
        String status,
        int attemptCount,
        Instant lastAttemptAt,
        Instant nextRetryAt,
        Instant deliveredAt,
        Integer responseStatusCode,
        String errorMessage,
        String payloadHash,
        Long payloadSizeBytes,
        Instant createdAt,
        Instant updatedAt
) {}
