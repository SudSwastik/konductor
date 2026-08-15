package com.konductor.projector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_trigger_selection")
public class EventTriggerSelection extends AuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "event_trigger_type_id")
    private Short eventTriggerTypeId;

    public Long getId() { return id; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Short getEventTriggerTypeId() { return eventTriggerTypeId; }
    public void setEventTriggerTypeId(Short eventTriggerTypeId) { this.eventTriggerTypeId = eventTriggerTypeId; }
}
