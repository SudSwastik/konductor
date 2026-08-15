package com.konductor.projector.dto;

import java.time.Instant;

public record SubscriptionSummaryResponse(
        String subscriptionUid,
        Short subscriptionTypeId,
        Short subscriptionStatusId,
        String name,
        String description,
        Instant activatedAt,
        Instant deactivatedAt,
        boolean active
) {
}
