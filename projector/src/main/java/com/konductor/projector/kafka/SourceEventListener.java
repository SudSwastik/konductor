package com.konductor.projector.kafka;

import com.konductor.projector.kafka.message.ProducerEventMessage;
import com.konductor.projector.service.SourceEventProjectionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SourceEventListener {
    private final SourceEventProjectionService sourceEventProjectionService;

    public SourceEventListener(SourceEventProjectionService sourceEventProjectionService) {
        this.sourceEventProjectionService = sourceEventProjectionService;
    }

    @KafkaListener(
            topics = "${konductor.kafka.source-events-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void receive(ProducerEventMessage message) {
        sourceEventProjectionService.project(message);
    }
}
