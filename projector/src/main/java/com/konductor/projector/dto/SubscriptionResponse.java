package com.konductor.projector.dto;

import java.util.List;

public record SubscriptionResponse(
        String subscriptionId,
        Integer subscriptionVersion,
        String subscriptionType,
        String status,
        SubscriptionBasicInfoResponse basicInfo,
        List<SubscriptionParameterResponse> parameters,
        List<SubscriptionTriggerResponse> triggers
) {
}
