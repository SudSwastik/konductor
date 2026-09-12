package com.konductor.projector.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionParameterRequest(
        @NotBlank String code
) {
}
