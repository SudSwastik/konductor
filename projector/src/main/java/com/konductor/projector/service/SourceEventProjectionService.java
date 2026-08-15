package com.konductor.projector.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.projector.entity.DeliveryConfig;
import com.konductor.projector.entity.Event;
import com.konductor.projector.entity.EventStatus;
import com.konductor.projector.entity.EventTriggerSelection;
import com.konductor.projector.entity.EventTriggerType;
import com.konductor.projector.kafka.message.ProducerEventMessage;
import com.konductor.projector.repository.DeliveryConfigRepository;
import com.konductor.projector.repository.EventRepository;
import com.konductor.projector.repository.EventStatusRepository;
import com.konductor.projector.repository.EventTriggerSelectionRepository;
import com.konductor.projector.repository.EventTriggerTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class SourceEventProjectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceEventProjectionService.class);
    private static final String DELIVERY_PENDING = "DELIVERY_PENDING";

    private final ObjectMapper objectMapper;
    private final EventRepository eventRepository;
    private final EventTriggerTypeRepository eventTriggerTypeRepository;
    private final EventTriggerSelectionRepository eventTriggerSelectionRepository;
    private final EventStatusRepository eventStatusRepository;
    private final DeliveryConfigRepository deliveryConfigRepository;

    public SourceEventProjectionService(
            ObjectMapper objectMapper,
            EventRepository eventRepository,
            EventTriggerTypeRepository eventTriggerTypeRepository,
            EventTriggerSelectionRepository eventTriggerSelectionRepository,
            EventStatusRepository eventStatusRepository,
            DeliveryConfigRepository deliveryConfigRepository
    ) {
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.eventTriggerTypeRepository = eventTriggerTypeRepository;
        this.eventTriggerSelectionRepository = eventTriggerSelectionRepository;
        this.eventStatusRepository = eventStatusRepository;
        this.deliveryConfigRepository = deliveryConfigRepository;
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
            deliveryConfigRepository.findFirstBySubscriptionIdAndActiveTrueOrderByIdDesc(selection.getSubscriptionId())
                    .map(DeliveryConfig::getId)
                    .ifPresent(event::setDeliveryConfigId);
            event.markCreated("SYSTEM");
            eventRepository.save(event);
        }
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
