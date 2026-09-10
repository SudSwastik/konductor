package com.konductor.projector.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionLifecycleTransitionRequest(
        @NotBlank String status
) {
}
