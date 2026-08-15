package com.konductor.producer.api;

import com.konductor.producer.dto.PublishEventRequest;
import com.konductor.producer.dto.PublishEventResponse;
import com.konductor.producer.service.EventProducerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventProducerController {
    private final EventProducerService eventProducerService;

    public EventProducerController(EventProducerService eventProducerService) {
        this.eventProducerService = eventProducerService;
    }

    @PostMapping("/{triggerType}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishEventResponse publishSample(@PathVariable String triggerType) {
        return eventProducerService.publishSample(triggerType);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishEventResponse publish(@Valid @RequestBody PublishEventRequest request) {
        return eventProducerService.publish(request);
    }
}
