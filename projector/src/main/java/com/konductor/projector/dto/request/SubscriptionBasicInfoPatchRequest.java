package com.konductor.projector.dto.request;

import java.time.LocalDate;

public record SubscriptionBasicInfoPatchRequest(
        String name,
        String description,
        LocalDate goLiveDate
) {
}
