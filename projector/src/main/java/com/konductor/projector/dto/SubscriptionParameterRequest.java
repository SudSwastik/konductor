package com.konductor.projector.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionParameterRequest(
        @NotBlank String code
) {
}
