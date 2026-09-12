package com.konductor.projector.dto.response;

public record SubscriptionSummaryResponse(
        String subscriptionId,
        Integer subscriptionVersion,
        String subscriptionType,
        String status,
        SubscriptionBasicInfoResponse basicInfo
) {
}
