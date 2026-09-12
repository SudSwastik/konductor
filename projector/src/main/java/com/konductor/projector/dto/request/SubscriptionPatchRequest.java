package com.konductor.projector.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPatchRequest(
        @Valid @NotNull SubscriptionBasicInfoPatchRequest basicInfo
) {
}
