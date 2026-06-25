package com.konductor.platform.subscription;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SubscriptionResponse {
    private UUID id;
    private String subscriberId;
    private String subscriberName;
    private String eventType;
    private List<String> fieldPaths;
    private String outputTopic;
    private boolean active;
    private int subscriptionVersion;
    private Instant createdAt;
    private Instant approvedAt;
}
