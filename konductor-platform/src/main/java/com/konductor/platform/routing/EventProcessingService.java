package com.konductor.platform.routing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.model.KonductorEvent;
import com.konductor.platform.dlq.DeadLetterService;
import com.konductor.platform.subscription.SubscriptionEntity;
import com.konductor.platform.subscription.SubscriptionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EventProcessingService {

    private final SubscriptionRepository subscriptionRepository;
    private final DeduplicationService deduplicationService;
    private final JsonPathFilterService jsonPathFilterService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DeadLetterService deadLetterService;
    private final ObjectMapper objectMapper;

    public EventProcessingService(SubscriptionRepository subscriptionRepository,
                                   DeduplicationService deduplicationService,
                                   JsonPathFilterService jsonPathFilterService,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   DeadLetterService deadLetterService,
                                   ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.deduplicationService = deduplicationService;
        this.jsonPathFilterService = jsonPathFilterService;
        this.kafkaTemplate = kafkaTemplate;
        this.deadLetterService = deadLetterService;
        this.objectMapper = objectMapper;
    }

    public void processEvent(KonductorEvent event) {
        String eventType = event.getMetadata().getEventType();
        List<SubscriptionEntity> subscriptions = subscriptionRepository.findByEventTypeAndActiveTrue(eventType);

        for (SubscriptionEntity subscription : subscriptions) {
            try {
                processForSubscription(event, subscription);
            } catch (Exception e) {
                deadLetterService.park(event, subscription.getSubscriberId(), e.getMessage());
            }
        }
    }

    private void processForSubscription(KonductorEvent event, SubscriptionEntity subscription)
            throws JsonProcessingException {
        if (deduplicationService.isDuplicate(subscription.getSubscriberId(), event.getMetadata().getEventId())) {
            return;
        }

        Map<String, Object> filteredPayload = jsonPathFilterService.filter(
                event.getPayload(), subscription.getFieldPaths());

        KonductorEvent filteredEvent = KonductorEvent.builder()
                .metadata(event.getMetadata())
                .payload(filteredPayload)
                .dataRefs(event.getDataRefs())
                .build();

        String eventJson = objectMapper.writeValueAsString(filteredEvent);
        kafkaTemplate.send(subscription.getOutputTopic(),
                event.getMetadata().getEventId().toString(),
                eventJson);
    }
}
