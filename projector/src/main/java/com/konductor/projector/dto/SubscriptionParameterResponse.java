package com.konductor.projector.dto;

public record SubscriptionParameterResponse(
        String code,
        String name,
        String description,
        String fieldPath,
        boolean required
) {
}
