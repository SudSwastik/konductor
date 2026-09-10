package com.konductor.projector.service;

import com.konductor.projector.dto.SubscriptionBasicInfoRequest;
import com.konductor.projector.dto.SubscriptionCreateRequest;
import com.konductor.projector.dto.SubscriptionParameterRequest;
import com.konductor.projector.dto.SubscriptionTriggerRequest;
import com.konductor.projector.entity.EventTriggerSelection;
import com.konductor.projector.entity.EventTriggerType;
import com.konductor.projector.entity.ParameterDefinition;
import com.konductor.projector.entity.Subscription;
import com.konductor.projector.entity.SubscriptionStatus;
import com.konductor.projector.entity.SubscriptionType;
import com.konductor.projector.repository.EventTriggerSelectionRepository;
import com.konductor.projector.repository.EventTriggerTypeRepository;
import com.konductor.projector.repository.ParameterDefinitionRepository;
import com.konductor.projector.repository.ParameterSelectionRepository;
import com.konductor.projector.repository.SubscriptionRepository;
import com.konductor.projector.repository.SubscriptionStatusRepository;
import com.konductor.projector.repository.SubscriptionTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionTypeRepository subscriptionTypeRepository;
    @Mock
    private SubscriptionStatusRepository subscriptionStatusRepository;
    @Mock
    private EventTriggerTypeRepository eventTriggerTypeRepository;
    @Mock
    private EventTriggerSelectionRepository eventTriggerSelectionRepository;
    @Mock
    private ParameterDefinitionRepository parameterDefinitionRepository;
    @Mock
    private ParameterSelectionRepository parameterSelectionRepository;
    @Mock
    private SubscriptionLifecyclePolicy lifecyclePolicy;
    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void derivesInternalFieldsWhenCreatingSubscription() {
        SubscriptionType subscriptionType = org.mockito.Mockito.mock(SubscriptionType.class);
        SubscriptionStatus activeStatus = org.mockito.Mockito.mock(SubscriptionStatus.class);
        when(subscriptionType.getId()).thenReturn((short) 2);
        when(subscriptionType.getCode()).thenReturn("EVENT");
        when(activeStatus.getId()).thenReturn((short) 1);
        when(activeStatus.getCode()).thenReturn("ACTIVE");
        when(subscriptionTypeRepository.findByCodeAndActiveTrue("EVENT")).thenReturn(Optional.of(subscriptionType));
        when(lifecyclePolicy.initialStatus(LocalDate.of(2026, 9, 15))).thenReturn("ACTIVE");
        when(subscriptionTypeRepository.findById((short) 2)).thenReturn(Optional.of(subscriptionType));
        when(subscriptionStatusRepository.findByCodeAndActiveTrue("ACTIVE")).thenReturn(Optional.of(activeStatus));
        when(subscriptionStatusRepository.findById((short) 1)).thenReturn(Optional.of(activeStatus));
        ParameterDefinition firstParameter = org.mockito.Mockito.mock(ParameterDefinition.class);
        ParameterDefinition secondParameter = org.mockito.Mockito.mock(ParameterDefinition.class);
        when(firstParameter.getId()).thenReturn(10L);
        when(secondParameter.getId()).thenReturn(11L);
        when(parameterDefinitionRepository.findByCodeInAndActiveTrue(List.of("ORDER_ID", "ORDER_STATUS")))
                .thenReturn(List.of(firstParameter, secondParameter));
        EventTriggerType firstTrigger = org.mockito.Mockito.mock(EventTriggerType.class);
        EventTriggerType secondTrigger = org.mockito.Mockito.mock(EventTriggerType.class);
        when(firstTrigger.getId()).thenReturn((short) 1);
        when(secondTrigger.getId()).thenReturn((short) 2);
        when(eventTriggerTypeRepository.findByCodeInAndActiveTrue(List.of("ORDER_CREATED", "ORDER_UPDATED")))
                .thenReturn(List.of(firstTrigger, secondTrigger));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 42L);
            return value;
        });
        when(eventTriggerSelectionRepository.findBySubscriptionId(42L)).thenReturn(new ArrayList<>());
        when(eventTriggerSelectionRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<EventTriggerSelection> values = invocation.getArgument(0);
            AtomicLong id = new AtomicLong(100L);
            List<EventTriggerSelection> saved = new ArrayList<>();
            values.forEach(value -> {
                ReflectionTestUtils.setField(value, "id", id.getAndIncrement());
                saved.add(value);
            });
            return saved;
        });
        when(parameterSelectionRepository.findByEventTriggerSelectionIdIn(any())).thenReturn(new ArrayList<>());
        when(eventTriggerSelectionRepository.findBySubscriptionIdAndActiveTrue(42L)).thenReturn(List.of());

        SubscriptionCreateRequest request = new SubscriptionCreateRequest(
                " event ",
                new SubscriptionBasicInfoRequest(
                        "Order fulfillment",
                        "Projects order changes",
                        LocalDate.of(2026, 9, 15)
                ),
                List.of(
                        new SubscriptionParameterRequest("order_id"),
                        new SubscriptionParameterRequest("order_status")
                ),
                List.of(
                        new SubscriptionTriggerRequest("order_created"),
                        new SubscriptionTriggerRequest("order_updated")
                )
        );

        subscriptionService.create(request, "owner@example.com");

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, org.mockito.Mockito.atLeastOnce()).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();
        assertThat(saved.getSubscriptionTypeId()).isEqualTo((short) 2);
        assertThat(saved.getSubscriptionStatusId()).isEqualTo((short) 1);
        assertThat(saved.getName()).isEqualTo("Order fulfillment");
        assertThat(saved.getDescription()).isEqualTo("Projects order changes");
        assertThat(saved.getActivatedAt()).isEqualTo(Instant.parse("2026-09-15T00:00:00Z"));
        assertThat(saved.getDeactivatedAt()).isNull();
        assertThat(saved.getSubscriptionUid()).startsWith("sub_");
        assertThat(saved.getCreatedBy()).isEqualTo("owner@example.com");
        assertThat(saved.getUpdatedBy()).isEqualTo("owner@example.com");

        verify(parameterSelectionRepository).saveAll(any());
        verify(eventTriggerSelectionRepository).saveAll(any());
    }

    @Test
    void rejectsUnknownSubscriptionTypeCode() {
        when(subscriptionTypeRepository.findByCodeAndActiveTrue("UNKNOWN")).thenReturn(Optional.empty());

        SubscriptionCreateRequest request = new SubscriptionCreateRequest(
                "unknown",
                new SubscriptionBasicInfoRequest("Example", null, LocalDate.of(2026, 9, 15)),
                List.of(new SubscriptionParameterRequest("ORDER_ID")),
                List.of(new SubscriptionTriggerRequest("ORDER_CREATED"))
        );

        assertThatThrownBy(() -> subscriptionService.create(request, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid subscriptionType");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void softDeletesSubscriptionAndDerivesArchivedStatus() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionUid("sub_test");
        subscription.setSubscriptionStatusId((short) 1);
        SubscriptionStatus activeStatus = org.mockito.Mockito.mock(SubscriptionStatus.class);
        when(activeStatus.getCode()).thenReturn("ACTIVE");
        SubscriptionStatus archivedStatus = org.mockito.Mockito.mock(SubscriptionStatus.class);
        when(archivedStatus.getId()).thenReturn((short) 4);
        when(subscriptionRepository.findBySubscriptionUid("sub_test"))
                .thenReturn(Optional.of(subscription));
        when(subscriptionStatusRepository.findById((short) 1)).thenReturn(Optional.of(activeStatus));
        when(subscriptionStatusRepository.findByCodeAndActiveTrue("ARCHIVED"))
                .thenReturn(Optional.of(archivedStatus));

        subscriptionService.softDelete("sub_test", "owner@example.com");

        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getSubscriptionStatusId()).isEqualTo((short) 4);
        assertThat(subscription.getUpdatedBy()).isEqualTo("owner@example.com");
        verify(subscriptionRepository).save(subscription);
    }
}
