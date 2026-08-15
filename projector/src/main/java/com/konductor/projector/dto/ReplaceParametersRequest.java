package com.konductor.projector.dto;

import jakarta.validation.Valid;

import java.util.List;

public record ReplaceParametersRequest(
        @Valid List<EventTriggerParameterRequest> parameters
) {
}
