package com.konductor.consumer.kafka;

import com.konductor.consumer.kafka.message.ProjectedEventMessage;
import com.konductor.consumer.service.ProjectedEventConsumerService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectedEventListener {
    private final ProjectedEventConsumerService projectedEventConsumerService;

    public ProjectedEventListener(ProjectedEventConsumerService projectedEventConsumerService) {
        this.projectedEventConsumerService = projectedEventConsumerService;
    }

    @KafkaListener(
            topics = "${konductor.kafka.subscription-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void receive(ProjectedEventMessage message) {
        projectedEventConsumerService.consume(message);
    }
}
