package com.konductor.projector.dto;

import java.time.Instant;
import java.util.List;

public record SubscriptionResponse(
        String subscriptionUid,
        Short subscriptionTypeId,
        Short subscriptionStatusId,
        String name,
        String description,
        Instant activatedAt,
        Instant deactivatedAt,
        boolean active,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        List<EventTriggerSelectionResponse> triggers,
        DeliveryConfigResponse deliveryConfig
) {
}
