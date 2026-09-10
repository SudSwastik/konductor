package com.konductor.projector.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionLifecycleScheduler {
    private final SubscriptionService subscriptionService;

    public SubscriptionLifecycleScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedDelayString = "${konductor.subscription.lifecycle.poll-interval-ms:60000}")
    public void activateDueSubscriptions() {
        subscriptionService.activateDueScheduledSubscriptions();
    }
}
