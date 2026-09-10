package com.konductor.projector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SubscriptionBasicInfoRequest(
        @NotBlank String name,
        String description,
        @NotNull LocalDate goLiveDate
) {
}
