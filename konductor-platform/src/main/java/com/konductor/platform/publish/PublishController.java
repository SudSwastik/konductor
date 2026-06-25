package com.konductor.platform.publish;

import com.konductor.model.KonductorEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PublishController {

    private final PublishService publishService;

    public PublishController(PublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, UUID>> publish(@RequestBody KonductorEvent event) {
        UUID eventId = publishService.publish(event);
        return ResponseEntity.accepted().body(Map.of("eventId", eventId));
    }
}
