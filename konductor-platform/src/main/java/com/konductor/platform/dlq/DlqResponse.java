package com.konductor.platform.dlq;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DlqResponse {
    private UUID id;
    private UUID eventId;
    private String eventType;
    private String originalEvent;
    private String subscriberId;
    private String failureReason;
    private int retryCount;
    private Instant lastRetryAt;
    private String status;
    private Instant createdAt;
}
