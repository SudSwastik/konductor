package com.konductor.projector.dto.response;

public record ParameterDefinitionResponse(
        String code,
        String dataType,
        String name,
        String description,
        String fieldPath,
        boolean required
) {
}
