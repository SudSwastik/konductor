package com.konductor.projector.service;

import com.konductor.projector.dto.request.*;
import com.konductor.projector.dto.response.*;
import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse create(SubscriptionCreateRequest request, String actor);
    List<SubscriptionSummaryResponse> list();
    SubscriptionResponse get(String subscriptionUid);
    SubscriptionResponse patchBasic(String subscriptionUid, SubscriptionPatchRequest request, String actor);
    SubscriptionResponse patchStatus(String subscriptionUid, SubscriptionStatusPatchRequest request, String actor);
    SubscriptionResponse transitionLifecycle(String subscriptionUid, String targetStatus, String actor);
    SubscriptionResponse replaceTriggers(String subscriptionUid, ReplaceTriggersRequest request, String actor);
    SubscriptionResponse replaceParameters(String subscriptionUid, ReplaceParametersRequest request, String actor);
    void softDelete(String subscriptionUid, String actor);
    int activateDueScheduledSubscriptions();
}
