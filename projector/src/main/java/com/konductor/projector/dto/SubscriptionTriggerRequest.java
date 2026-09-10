package com.konductor.projector.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionTriggerRequest(
        @NotBlank String code
) {
}
