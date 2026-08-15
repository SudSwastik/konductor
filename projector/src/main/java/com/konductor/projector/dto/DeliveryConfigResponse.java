package com.konductor.projector.dto;

public record DeliveryConfigResponse(
        String deliveryType,
        String endpointUrl,
        String httpMethod,
        int timeoutSeconds,
        int maxRetryCount,
        int retryBackoffSeconds
) {
}
