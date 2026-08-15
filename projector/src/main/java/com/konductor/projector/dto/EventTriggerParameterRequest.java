package com.konductor.projector.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EventTriggerParameterRequest(
        @NotNull Short eventTriggerTypeId,
        List<Long> parameterDefinitionIds
) {
}
