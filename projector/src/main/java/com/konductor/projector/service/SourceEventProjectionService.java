package com.konductor.projector.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.projector.entity.DeliveryConfig;
import com.konductor.projector.entity.Event;
import com.konductor.projector.entity.EventStatus;
import com.konductor.projector.entity.EventTriggerSelection;
import com.konductor.projector.entity.EventTriggerType;
import com.konductor.projector.entity.ParameterDefinition;
import com.konductor.projector.entity.ParameterSelection;
import com.konductor.projector.entity.Subscription;
import com.konductor.projector.kafka.message.ProjectedEventMessage;
import com.konductor.projector.kafka.message.ProducerEventMessage;
import com.konductor.projector.repository.DeliveryConfigRepository;
import com.konductor.projector.repository.EventRepository;
import com.konductor.projector.repository.EventStatusRepository;
import com.konductor.projector.repository.EventTriggerSelectionRepository;
import com.konductor.projector.repository.EventTriggerTypeRepository;
import com.konductor.projector.repository.ParameterDefinitionRepository;
import com.konductor.projector.repository.ParameterSelectionRepository;
import com.konductor.projector.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SourceEventProjectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceEventProjectionService.class);
    private static final String DELIVERY_PENDING = "DELIVERY_PENDING";
    private static final String DELIVERY_IN_PROGRESS = "DELIVERY_IN_PROGRESS";

    private final ObjectMapper objectMapper;
    private final EventRepository eventRepository;
    private final EventTriggerTypeRepository eventTriggerTypeRepository;
    private final EventTriggerSelectionRepository eventTriggerSelectionRepository;
    private final EventStatusRepository eventStatusRepository;
    private final DeliveryConfigRepository deliveryConfigRepository;
    private final ParameterSelectionRepository parameterSelectionRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ProjectedEventPublisher projectedEventPublisher;
    private final String subscriptionTopicPrefix;

    public SourceEventProjectionService(
            ObjectMapper objectMapper,
            EventRepository eventRepository,
            EventTriggerTypeRepository eventTriggerTypeRepository,
            EventTriggerSelectionRepository eventTriggerSelectionRepository,
            EventStatusRepository eventStatusRepository,
            DeliveryConfigRepository deliveryConfigRepository,
            ParameterSelectionRepository parameterSelectionRepository,
            ParameterDefinitionRepository parameterDefinitionRepository,
            SubscriptionRepository subscriptionRepository,
            ProjectedEventPublisher projectedEventPublisher,
            @Value("${konductor.kafka.subscription-topic-prefix}") String subscriptionTopicPrefix
    ) {
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.eventTriggerTypeRepository = eventTriggerTypeRepository;
        this.eventTriggerSelectionRepository = eventTriggerSelectionRepository;
        this.eventStatusRepository = eventStatusRepository;
        this.deliveryConfigRepository = deliveryConfigRepository;
        this.parameterSelectionRepository = parameterSelectionRepository;
        this.parameterDefinitionRepository = parameterDefinitionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.projectedEventPublisher = projectedEventPublisher;
        this.subscriptionTopicPrefix = subscriptionTopicPrefix;
    }

    @Transactional
    public void project(ProducerEventMessage message) {
        EventTriggerType triggerType = eventTriggerTypeRepository.findByCodeAndActiveTrue(message.triggerType())
                .orElse(null);
        if (triggerType == null) {
            LOGGER.warn("Ignoring source event {} because trigger type {} is not configured",
                    message.sourceEventId(),
                    message.triggerType());
            return;
        }

        EventStatus deliveryPending = eventStatusRepository.findByCodeAndActiveTrue(DELIVERY_PENDING)
                .orElseThrow(() -> new IllegalStateException("Missing event status " + DELIVERY_PENDING));
        EventStatus deliveryInProgress = eventStatusRepository.findByCodeAndActiveTrue(DELIVERY_IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("Missing event status " + DELIVERY_IN_PROGRESS));
        List<EventTriggerSelection> selections =
                eventTriggerSelectionRepository.findByEventTriggerTypeIdAndActiveTrue(triggerType.getId());
        if (selections.isEmpty()) {
            LOGGER.info("No active subscriptions for source event {} trigger {}",
                    message.sourceEventId(),
                    message.triggerType());
            return;
        }

        String payloadJson = payloadJson(message);
        String payloadHash = sha256(payloadJson);
        long payloadSizeBytes = payloadJson.getBytes(StandardCharsets.UTF_8).length;

        for (EventTriggerSelection selection : selections) {
            Event event = new Event();
            event.setEventUid(newEventUid());
            event.setSourceEventId(message.sourceEventId());
            event.setEventTriggerTypeId(triggerType.getId());
            event.setSubscriptionId(selection.getSubscriptionId());
            event.setEventTriggerSelectionId(selection.getId());
            event.setEventStatusId(deliveryPending.getId());
            event.setAttemptCount(0);
            event.setPayloadHash(payloadHash);
            event.setPayloadSizeBytes(payloadSizeBytes);
            DeliveryConfig deliveryConfig = deliveryConfigRepository
                    .findFirstBySubscriptionIdAndActiveTrueOrderByIdDesc(selection.getSubscriptionId())
                    .orElse(null);
            if (deliveryConfig != null) {
                event.setDeliveryConfigId(deliveryConfig.getId());
            }
            event.markCreated("SYSTEM");
            event = eventRepository.save(event);

            Subscription subscription = subscriptionRepository.findById(selection.getSubscriptionId())
                    .orElseThrow(() -> new IllegalStateException("Missing subscription " + selection.getSubscriptionId()));
            String topic = topicFor(subscription.getSubscriptionUid(), deliveryConfig);
            ProjectedEventMessage projectedMessage = new ProjectedEventMessage(
                    event.getEventUid(),
                    message.sourceEventId(),
                    subscription.getSubscriptionUid(),
                    message.triggerType(),
                    Instant.now(),
                    projectedData(selection.getId(), message.body())
            );
            projectedEventPublisher.publish(topic, projectedMessage);
            event.setEventStatusId(deliveryInProgress.getId());
            event.markUpdated("SYSTEM");
            eventRepository.save(event);
        }
    }

    private Map<String, Object> projectedData(Long eventTriggerSelectionId, Map<String, Object> body) {
        List<ParameterSelection> selections =
                parameterSelectionRepository.findByEventTriggerSelectionIdAndActiveTrue(eventTriggerSelectionId);
        List<Long> parameterDefinitionIds = selections.stream()
                .map(ParameterSelection::getParameterDefinitionId)
                .toList();
        if (parameterDefinitionIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> projected = new LinkedHashMap<>();
        List<ParameterDefinition> definitions =
                parameterDefinitionRepository.findByIdInAndActiveTrue(parameterDefinitionIds);
        for (ParameterDefinition definition : definitions) {
            projected.put(definition.getFieldPath(), valueAt(body, definition.getFieldPath()));
        }
        return projected;
    }

    private Object valueAt(Map<String, Object> body, String fieldPath) {
        if (body == null || fieldPath == null || !fieldPath.startsWith("$.")) {
            return null;
        }

        Object current = body;
        for (String rawSegment : fieldPath.substring(2).split("\\.")) {
            if (current == null) {
                return null;
            }
            if (rawSegment.endsWith("length()")) {
                String collectionName = rawSegment.substring(0, rawSegment.length() - ".length()".length());
                current = childValue(current, collectionName);
                return current instanceof Collection<?> collection ? collection.size() : null;
            }

            current = segmentValue(current, rawSegment);
        }
        return current;
    }

    private Object segmentValue(Object current, String segment) {
        if (segment.endsWith("]")) {
            int bracketStart = segment.indexOf('[');
            if (bracketStart > 0) {
                Object arrayValue = childValue(current, segment.substring(0, bracketStart));
                int index = Integer.parseInt(segment.substring(bracketStart + 1, segment.length() - 1));
                if (arrayValue instanceof List<?> list && index >= 0 && index < list.size()) {
                    return list.get(index);
                }
                return null;
            }
        }
        return childValue(current, segment);
    }

    private Object childValue(Object current, String key) {
        if (current instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private String topicFor(String subscriptionUid, DeliveryConfig deliveryConfig) {
        if (deliveryConfig != null
                && deliveryConfig.getEndpointUrl() != null
                && !deliveryConfig.getEndpointUrl().isBlank()) {
            return deliveryConfig.getEndpointUrl();
        }
        return subscriptionTopicPrefix + "." + subscriptionUid;
    }

    private String payloadJson(ProducerEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message.body());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize source event body", exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String newEventUid() {
        return "evt_" + UUID.randomUUID().toString().replace("-", "");
    }
}
