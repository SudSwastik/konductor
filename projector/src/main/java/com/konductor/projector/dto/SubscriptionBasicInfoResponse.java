package com.konductor.projector.dto;

import java.time.LocalDate;

public record SubscriptionBasicInfoResponse(
        String name,
        String description,
        LocalDate goLiveDate
) {
}
