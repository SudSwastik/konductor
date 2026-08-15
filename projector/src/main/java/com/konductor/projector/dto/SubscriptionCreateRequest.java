package com.konductor.projector.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record SubscriptionCreateRequest(
        @NotNull Short subscriptionTypeId,
        @NotNull Short subscriptionStatusId,
        @NotBlank String name,
        String description,
        Instant activatedAt,
        Instant deactivatedAt,
        @Valid List<EventTriggerParameterRequest> triggers,
        @Valid DeliveryConfigRequest deliveryConfig
) {
}
