package com.konductor.projector.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "subscription_status")
public class SubscriptionStatus extends AuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    private String code;
    private String name;
    private String description;

    public Short getId() { return id; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
