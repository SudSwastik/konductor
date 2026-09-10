package com.konductor.projector.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionStatusPatchRequest(
        @NotBlank String status
) {
}
