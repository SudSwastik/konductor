package com.konductor.platform.dlq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.model.KonductorEvent;
import com.konductor.platform.routing.EventProcessingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeadLetterService {

    private final DeadLetterEventRepository repository;
    private final ObjectMapper objectMapper;
    private final EventProcessingService processingService;

    public DeadLetterService(DeadLetterEventRepository repository,
                              ObjectMapper objectMapper,
                              @Lazy EventProcessingService processingService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.processingService = processingService;
    }

    public List<DeadLetterEventEntity> findByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<DeadLetterEventEntity> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public void park(KonductorEvent event, String subscriberId, String reason) {
        DeadLetterEventEntity entity = DeadLetterEventEntity.builder()
                .eventId(event.getMetadata().getEventId())
                .eventType(event.getMetadata().getEventType())
                .originalEvent(toJson(event))
                .subscriberId(subscriberId)
                .failureReason(reason)
                .build();
        repository.save(entity);
    }

    public void retry(UUID dlqId) {
        DeadLetterEventEntity entity = repository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: " + dlqId));

        KonductorEvent event = fromJson(entity.getOriginalEvent());
        processingService.processEvent(event);

        entity.setRetryCount(entity.getRetryCount() + 1);
        entity.setLastRetryAt(Instant.now());
        entity.setStatus("RETRIED");
        repository.save(entity);
    }

    public void discard(UUID dlqId) {
        DeadLetterEventEntity entity = repository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: " + dlqId));
        entity.setStatus("DISCARDED");
        repository.save(entity);
    }

    private String toJson(KonductorEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event for DLQ", e);
        }
    }

    private KonductorEvent fromJson(String json) {
        try {
            return objectMapper.readValue(json, KonductorEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize DLQ event", e);
        }
    }
}
