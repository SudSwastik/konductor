package com.konductor.producer.service;

import com.konductor.producer.dto.PublishEventRequest;
import com.konductor.producer.dto.PublishEventResponse;
import com.konductor.producer.kafka.message.ProducerEventMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class EventProducerService {
    private static final String SOURCE_SYSTEM = "konductor-producer";

    private final KafkaTemplate<String, ProducerEventMessage> kafkaTemplate;
    private final SampleEventFactory sampleEventFactory;
    private final String sourceEventsTopic;

    public EventProducerService(
            KafkaTemplate<String, ProducerEventMessage> kafkaTemplate,
            SampleEventFactory sampleEventFactory,
            @Value("${konductor.kafka.source-events-topic}") String sourceEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.sampleEventFactory = sampleEventFactory;
        this.sourceEventsTopic = sourceEventsTopic;
    }

    public PublishEventResponse publishSample(String triggerType) {
        return publish(new PublishEventRequest(
                triggerType,
                null,
                null,
                null,
                null,
                sampleEventFactory.bodyFor(triggerType)
        ));
    }

    public PublishEventResponse publish(PublishEventRequest request) {
        String sourceEventId = blankToDefault(request.sourceEventId(), "src_" + UUID.randomUUID().toString().replace("-", ""));
        Instant occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
        String sourceSystem = blankToDefault(request.sourceSystem(), SOURCE_SYSTEM);
        String correlationId = blankToDefault(request.correlationId(), "corr_" + UUID.randomUUID().toString().replace("-", ""));
        ProducerEventMessage message = new ProducerEventMessage(
                sourceEventId,
                request.triggerType(),
                occurredAt,
                sourceSystem,
                correlationId,
                request.body()
        );
        kafkaTemplate.send(sourceEventsTopic, sourceEventId, message);
        return new PublishEventResponse(sourceEventsTopic, sourceEventId, request.triggerType(), occurredAt);
    }

    private String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
