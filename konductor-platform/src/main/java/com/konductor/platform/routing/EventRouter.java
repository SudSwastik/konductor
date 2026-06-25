package com.konductor.platform.routing;

import com.konductor.model.KonductorEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventRouter {

    private final EventProcessingService eventProcessingService;

    public EventRouter(EventProcessingService eventProcessingService) {
        this.eventProcessingService = eventProcessingService;
    }

    @KafkaListener(topics = "${konductor.kafka.internal-topic}")
    public void onEvent(KonductorEvent event) {
        eventProcessingService.processEvent(event);
    }
}
