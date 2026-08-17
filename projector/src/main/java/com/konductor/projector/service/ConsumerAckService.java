package com.konductor.projector.service;

import com.konductor.projector.entity.Event;
import com.konductor.projector.entity.EventStatus;
import com.konductor.projector.kafka.message.ConsumerAckMessage;
import com.konductor.projector.repository.EventRepository;
import com.konductor.projector.repository.EventStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ConsumerAckService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerAckService.class);
    private static final String ACKED = "ACKED";
    private static final String DELIVERED = "DELIVERED";
    private static final String DELIVERY_FAILED = "DELIVERY_FAILED";

    private final EventRepository eventRepository;
    private final EventStatusRepository eventStatusRepository;

    public ConsumerAckService(EventRepository eventRepository, EventStatusRepository eventStatusRepository) {
        this.eventRepository = eventRepository;
        this.eventStatusRepository = eventStatusRepository;
    }

    @Transactional
    public void apply(ConsumerAckMessage ack) {
        Event event = eventRepository.findByEventUidAndActiveTrue(ack.eventUid()).orElse(null);
        if (event == null) {
            LOGGER.warn("Ignoring ack for unknown event {}", ack.eventUid());
            return;
        }

        boolean success = ACKED.equalsIgnoreCase(ack.status());
        EventStatus status = eventStatusRepository.findByCodeAndActiveTrue(success ? DELIVERED : DELIVERY_FAILED)
                .orElseThrow(() -> new IllegalStateException("Missing event status for consumer ack"));
        Instant acknowledgedAt = ack.acknowledgedAt() == null ? Instant.now() : ack.acknowledgedAt();

        event.setEventStatusId(status.getId());
        event.setLastAttemptAt(acknowledgedAt);
        event.setErrorMessage(success ? null : ack.message());
        if (success) {
            event.setDeliveredAt(acknowledgedAt);
        }
        event.markUpdated(ack.consumerName() == null || ack.consumerName().isBlank() ? "SYSTEM" : ack.consumerName());
        eventRepository.save(event);
    }
}
