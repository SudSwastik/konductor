package com.konductor.projector.service;

import com.konductor.projector.kafka.message.ProjectedEventMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProjectedEventPublisher {
    private final KafkaTemplate<String, ProjectedEventMessage> kafkaTemplate;

    public ProjectedEventPublisher(KafkaTemplate<String, ProjectedEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, ProjectedEventMessage message) {
        kafkaTemplate.send(topic, message.eventUid(), message);
    }
}
