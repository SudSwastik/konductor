package com.konductor.projector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event")
public class Event extends MutableAuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_uid")
    private String eventUid;

    @Column(name = "source_event_id")
    private String sourceEventId;

    @Column(name = "event_trigger_type_id")
    private Short eventTriggerTypeId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "event_trigger_selection_id")
    private Long eventTriggerSelectionId;

    @Column(name = "delivery_config_id")
    private Long deliveryConfigId;

    @Column(name = "event_status_id")
    private Short eventStatusId;

    @Column(name = "attempt_count")
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Column(name = "payload_size_bytes")
    private Long payloadSizeBytes;

    public Long getId() { return id; }
    public String getEventUid() { return eventUid; }
    public void setEventUid(String eventUid) { this.eventUid = eventUid; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
    public Short getEventTriggerTypeId() { return eventTriggerTypeId; }
    public void setEventTriggerTypeId(Short eventTriggerTypeId) { this.eventTriggerTypeId = eventTriggerTypeId; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Long getEventTriggerSelectionId() { return eventTriggerSelectionId; }
    public void setEventTriggerSelectionId(Long eventTriggerSelectionId) { this.eventTriggerSelectionId = eventTriggerSelectionId; }
    public Long getDeliveryConfigId() { return deliveryConfigId; }
    public void setDeliveryConfigId(Long deliveryConfigId) { this.deliveryConfigId = deliveryConfigId; }
    public Short getEventStatusId() { return eventStatusId; }
    public void setEventStatusId(Short eventStatusId) { this.eventStatusId = eventStatusId; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public Integer getResponseStatusCode() { return responseStatusCode; }
    public void setResponseStatusCode(Integer responseStatusCode) { this.responseStatusCode = responseStatusCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public Long getPayloadSizeBytes() { return payloadSizeBytes; }
    public void setPayloadSizeBytes(Long payloadSizeBytes) { this.payloadSizeBytes = payloadSizeBytes; }
}
