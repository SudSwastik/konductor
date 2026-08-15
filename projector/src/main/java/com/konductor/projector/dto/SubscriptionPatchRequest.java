package com.konductor.projector.dto;

import java.time.Instant;

public record SubscriptionPatchRequest(
        Short subscriptionTypeId,
        Short subscriptionStatusId,
        String name,
        String description,
        Instant activatedAt,
        Instant deactivatedAt
) {
}
