package com.konductor.projector.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DeliveryConfigRequest(
        @NotBlank String deliveryType,
        String endpointUrl,
        String httpMethod,
        @Min(1) Integer timeoutSeconds,
        @Min(0) Integer maxRetryCount,
        @Min(1) Integer retryBackoffSeconds
) {
}
