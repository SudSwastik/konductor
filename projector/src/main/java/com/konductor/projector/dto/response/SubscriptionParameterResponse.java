package com.konductor.projector.dto.response;

public record SubscriptionParameterResponse(
        String code,
        String name,
        String description,
        String fieldPath,
        boolean required
) {
}
