package com.konductor.projector.dto;

public record ParameterDefinitionResponse(
        Long id,
        Short dataTypeId,
        String name,
        String description,
        String fieldPath,
        boolean required
) {
}
