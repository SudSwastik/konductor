package com.konductor.projector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "subscription")
public class Subscription extends MutableAuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_uid")
    private String subscriptionUid;

    @Column(name = "subscription_type_id")
    private Short subscriptionTypeId;

    @Column(name = "subscription_status_id")
    private Short subscriptionStatusId;

    private String name;
    private String description;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    public Long getId() { return id; }
    public String getSubscriptionUid() { return subscriptionUid; }
    public void setSubscriptionUid(String subscriptionUid) { this.subscriptionUid = subscriptionUid; }
    public Short getSubscriptionTypeId() { return subscriptionTypeId; }
    public void setSubscriptionTypeId(Short subscriptionTypeId) { this.subscriptionTypeId = subscriptionTypeId; }
    public Short getSubscriptionStatusId() { return subscriptionStatusId; }
    public void setSubscriptionStatusId(Short subscriptionStatusId) { this.subscriptionStatusId = subscriptionStatusId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }
}
