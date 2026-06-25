package com.konductor.platform.subscription;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
public class AdminSubscriptionController {

    private final SubscriptionRepository subscriptionRepository;

    public AdminSubscriptionController(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping
    public List<SubscriptionResponse> list(@RequestParam(defaultValue = "ALL") String status) {
        List<SubscriptionEntity> entities = switch (status.toUpperCase()) {
            case "APPROVED" -> subscriptionRepository.findByActiveTrue();
            case "PENDING"  -> subscriptionRepository.findByActiveFalse();
            default         -> subscriptionRepository.findAll();
        };
        return entities.stream().map(AdminSubscriptionController::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> register(@Valid @RequestBody RegisterSubscriptionRequest request) {
        SubscriptionEntity entity = SubscriptionEntity.builder()
                .subscriberId(request.getSubscriberId())
                .subscriberName(request.getSubscriberName())
                .eventType(request.getEventType())
                .fieldPaths(request.getFieldPaths())
                .outputTopic(request.getOutputTopic())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(subscriptionRepository.save(entity)));
    }

    @PutMapping("/{id}/approve")
    public SubscriptionResponse approve(@PathVariable UUID id) {
        SubscriptionEntity entity = findOrThrow(id);
        entity.setActive(true);
        entity.setApprovedAt(Instant.now());
        entity.setSubscriptionVersion(entity.getSubscriptionVersion() + 1);
        return toResponse(subscriptionRepository.save(entity));
    }

    @PutMapping("/{id}/reject")
    public SubscriptionResponse reject(@PathVariable UUID id) {
        SubscriptionEntity entity = findOrThrow(id);
        entity.setActive(false);
        return toResponse(subscriptionRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        SubscriptionEntity entity = findOrThrow(id);
        entity.setActive(false);
        subscriptionRepository.save(entity);
    }

    private SubscriptionEntity findOrThrow(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscription not found: " + id));
    }

    static SubscriptionResponse toResponse(SubscriptionEntity e) {
        return SubscriptionResponse.builder()
                .id(e.getId())
                .subscriberId(e.getSubscriberId())
                .subscriberName(e.getSubscriberName())
                .eventType(e.getEventType())
                .fieldPaths(e.getFieldPaths())
                .outputTopic(e.getOutputTopic())
                .active(e.isActive())
                .subscriptionVersion(e.getSubscriptionVersion())
                .createdAt(e.getCreatedAt())
                .approvedAt(e.getApprovedAt())
                .build();
    }
}
