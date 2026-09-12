package com.konductor.projector.service;

import com.konductor.projector.dto.EventResponse;
import com.konductor.projector.entity.Event;
import com.konductor.projector.repository.EventRepository;
import com.konductor.projector.repository.EventStatusRepository;
import com.konductor.projector.repository.EventTriggerTypeRepository;
import com.konductor.projector.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EventService {
    private final EventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventTriggerTypeRepository eventTriggerTypeRepository;
    private final EventStatusRepository eventStatusRepository;

    public EventService(
            EventRepository eventRepository,
            SubscriptionRepository subscriptionRepository,
            EventTriggerTypeRepository eventTriggerTypeRepository,
            EventStatusRepository eventStatusRepository
    ) {
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventTriggerTypeRepository = eventTriggerTypeRepository;
        this.eventStatusRepository = eventStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        return eventRepository.findAllByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(String eventUid) {
        return eventRepository.findByEventUidAndActiveTrue(eventUid)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Event not found"));
    }

    private EventResponse toResponse(Event event) {
        String subscriptionUid = subscriptionRepository.findById(event.getSubscriptionId())
                .map(subscription -> subscription.getSubscriptionUid())
                .orElse(null);
        String triggerType = eventTriggerTypeRepository.findById(event.getEventTriggerTypeId())
                .map(value -> value.getCode())
                .orElse(null);
        String status = eventStatusRepository.findById(event.getEventStatusId())
                .map(value -> value.getCode())
                .orElse(null);
        return new EventResponse(
                event.getEventUid(), event.getSourceEventId(), subscriptionUid, triggerType, status,
                event.getAttemptCount(), event.getLastAttemptAt(), event.getNextRetryAt(), event.getDeliveredAt(),
                event.getResponseStatusCode(), event.getErrorMessage(), event.getPayloadHash(), event.getPayloadSizeBytes(),
                event.getCreatedAt(), event.getUpdatedAt()
        );
    }
}
