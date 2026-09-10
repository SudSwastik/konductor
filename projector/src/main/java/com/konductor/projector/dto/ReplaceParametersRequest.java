package com.konductor.projector.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceParametersRequest(
        @NotEmpty List<@Valid @NotNull SubscriptionParameterRequest> parameters
) {
}
