package com.konductor.platform.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.konductor.model.EventMetadata;
import com.konductor.model.KonductorEvent;
import com.konductor.model.KonductorEventBuilder;
import com.konductor.platform.dlq.DeadLetterService;
import com.konductor.platform.subscription.SubscriptionEntity;
import com.konductor.platform.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private DeduplicationService deduplicationService;
    @Mock
    private JsonPathFilterService jsonPathFilterService;
    @SuppressWarnings("unchecked")
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private DeadLetterService deadLetterService;

    private EventProcessingService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new EventProcessingService(
                subscriptionRepository, deduplicationService, jsonPathFilterService,
                kafkaTemplate, deadLetterService, objectMapper);
    }

    @Test
    void processEvent_sendsFilteredEventToOutputTopic() {
        KonductorEvent event = testEvent("order.created");
        SubscriptionEntity sub = subscription("sub-a", "order.created", "sub-a-events");

        when(subscriptionRepository.findByEventTypeAndActiveTrue("order.created")).thenReturn(List.of(sub));
        when(deduplicationService.isDuplicate(anyString(), any())).thenReturn(false);
        when(jsonPathFilterService.filter(any(), any())).thenReturn(Map.of("orderId", "abc"));

        service.processEvent(event);

        verify(kafkaTemplate).send(eq("sub-a-events"), anyString(), anyString());
    }

    @Test
    void processEvent_skipsEvent_whenDuplicate() {
        KonductorEvent event = testEvent("order.created");
        SubscriptionEntity sub = subscription("sub-b", "order.created", "sub-b-events");

        when(subscriptionRepository.findByEventTypeAndActiveTrue("order.created")).thenReturn(List.of(sub));
        when(deduplicationService.isDuplicate(anyString(), any())).thenReturn(true);

        service.processEvent(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void processEvent_parksInDlq_onException() {
        KonductorEvent event = testEvent("order.created");
        SubscriptionEntity sub = subscription("sub-c", "order.created", "sub-c-events");

        when(subscriptionRepository.findByEventTypeAndActiveTrue("order.created")).thenReturn(List.of(sub));
        when(deduplicationService.isDuplicate(anyString(), any())).thenReturn(false);
        when(jsonPathFilterService.filter(any(), any())).thenThrow(new RuntimeException("filter failed"));

        service.processEvent(event);

        verify(deadLetterService).park(eq(event), eq("sub-c"), anyString());
    }

    @Test
    void processEvent_doesNothing_whenNoSubscriptions() {
        KonductorEvent event = testEvent("order.created");
        when(subscriptionRepository.findByEventTypeAndActiveTrue("order.created")).thenReturn(List.of());

        service.processEvent(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verifyNoInteractions(deduplicationService, deadLetterService);
    }

    @Test
    void processEvent_fanOut_toMultipleSubscribers() {
        KonductorEvent event = testEvent("payment.processed");
        SubscriptionEntity sub1 = subscription("sub-d", "payment.processed", "sub-d-events");
        SubscriptionEntity sub2 = subscription("sub-e", "payment.processed", "sub-e-events");

        when(subscriptionRepository.findByEventTypeAndActiveTrue("payment.processed"))
                .thenReturn(List.of(sub1, sub2));
        when(deduplicationService.isDuplicate(anyString(), any())).thenReturn(false);
        when(jsonPathFilterService.filter(any(), any())).thenReturn(Map.of("amount", 100));

        service.processEvent(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), anyString(), anyString());
        assertThat(topicCaptor.getAllValues()).containsExactlyInAnyOrder("sub-d-events", "sub-e-events");
    }

    private KonductorEvent testEvent(String eventType) {
        return KonductorEventBuilder.newEvent()
                .eventType(eventType)
                .sourceConfigId("test-service")
                .payload(Map.of("orderId", "abc", "amount", 99.5))
                .build();
    }

    private SubscriptionEntity subscription(String subscriberId, String eventType, String outputTopic) {
        return SubscriptionEntity.builder()
                .subscriberId(subscriberId)
                .subscriberName(subscriberId + "-service")
                .eventType(eventType)
                .fieldPaths(List.of("$.orderId", "$.amount"))
                .outputTopic(outputTopic)
                .active(true)
                .build();
    }
}
