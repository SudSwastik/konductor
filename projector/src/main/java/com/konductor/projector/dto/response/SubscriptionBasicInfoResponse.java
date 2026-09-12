package com.konductor.projector.dto.response;

import java.time.LocalDate;

public record SubscriptionBasicInfoResponse(
        String name,
        String description,
        LocalDate goLiveDate
) {
}
