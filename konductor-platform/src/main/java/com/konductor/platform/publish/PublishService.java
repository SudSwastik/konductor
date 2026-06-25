package com.konductor.platform.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.model.KonductorEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PublishService {

    private final SchemaRegistryValidator validator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${konductor.kafka.internal-topic}")
    private String internalTopic;

    public PublishService(SchemaRegistryValidator validator,
                          KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public UUID publish(KonductorEvent event) {
        validator.validate(event);
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(internalTopic, event.getMetadata().getEventId().toString(), json);
            return event.getMetadata().getEventId();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event for Kafka", e);
        }
    }
}
