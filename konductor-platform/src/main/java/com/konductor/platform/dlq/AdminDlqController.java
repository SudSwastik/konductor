package com.konductor.platform.dlq;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dlq")
public class AdminDlqController {

    private final DeadLetterService deadLetterService;

    public AdminDlqController(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @GetMapping
    public List<DlqResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType) {
        List<DeadLetterEventEntity> entities = status != null
                ? deadLetterService.findByStatus(status)
                : deadLetterService.findAll();

        if (eventType != null) {
            entities = entities.stream()
                    .filter(e -> eventType.equals(e.getEventType()))
                    .toList();
        }

        return entities.stream().map(AdminDlqController::toResponse).toList();
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable UUID id) {
        deadLetterService.retry(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<Void> discard(@PathVariable UUID id) {
        deadLetterService.discard(id);
        return ResponseEntity.ok().build();
    }

    static DlqResponse toResponse(DeadLetterEventEntity e) {
        return DlqResponse.builder()
                .id(e.getId())
                .eventId(e.getEventId())
                .eventType(e.getEventType())
                .originalEvent(e.getOriginalEvent())
                .subscriberId(e.getSubscriberId())
                .failureReason(e.getFailureReason())
                .retryCount(e.getRetryCount())
                .lastRetryAt(e.getLastRetryAt())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
