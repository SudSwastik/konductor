package com.konductor.projector.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionLifecycleTransitionRequest(
        @NotBlank String status
) {
}
