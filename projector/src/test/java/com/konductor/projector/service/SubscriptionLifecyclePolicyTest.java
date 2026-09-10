package com.konductor.projector.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionLifecyclePolicyTest {
    private final SubscriptionLifecyclePolicy policy = new SubscriptionLifecyclePolicy();

    @Test
    void futureGoLiveStartsScheduled() {
        assertThat(policy.initialStatus(LocalDate.now().plusDays(1)))
                .isEqualTo(SubscriptionLifecyclePolicy.SCHEDULED);
    }

    @Test
    void scheduledSubscriptionCanBeRescheduled() {
        assertThatCode(() -> policy.validateTransition(
                SubscriptionLifecyclePolicy.SCHEDULED,
                SubscriptionLifecyclePolicy.SCHEDULED
        )).doesNotThrowAnyException();
    }

    @Test
    void archivedSubscriptionCanOnlyBeUnarchivedToInactive() {
        assertThatCode(() -> policy.validateTransition(
                SubscriptionLifecyclePolicy.ARCHIVED,
                SubscriptionLifecyclePolicy.INACTIVE
        )).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateTransition(
                SubscriptionLifecyclePolicy.ARCHIVED,
                SubscriptionLifecyclePolicy.ACTIVE
        )).isInstanceOf(ResponseStatusException.class);
    }
}
