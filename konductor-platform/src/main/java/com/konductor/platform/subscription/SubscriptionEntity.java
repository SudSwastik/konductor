package com.konductor.platform.subscription;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscriber_id", nullable = false)
    private String subscriberId;

    @Column(name = "subscriber_name", nullable = false)
    private String subscriberName;

    @Builder.Default
    @Column(name = "subscription_version")
    private Integer subscriptionVersion = 1;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_paths", nullable = false, columnDefinition = "jsonb")
    private List<String> fieldPaths;

    @Column(name = "output_topic", nullable = false)
    private String outputTopic;

    @Builder.Default
    @Column(name = "active")
    private boolean active = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
