package com.konductor.platform.dlq;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "original_event", columnDefinition = "jsonb")
    private String originalEvent;

    @Column(name = "subscriber_id")
    private String subscriberId;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Builder.Default
    @Column(name = "status")
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
