package com.konductor.projector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parameter_selection")
public class ParameterSelection extends AuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "event_trigger_selection_id")
    private Long eventTriggerSelectionId;

    @Column(name = "parameter_definition_id")
    private Long parameterDefinitionId;

    public Long getId() { return id; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getEventTriggerSelectionId() { return eventTriggerSelectionId; }
    public void setEventTriggerSelectionId(Long eventTriggerSelectionId) { this.eventTriggerSelectionId = eventTriggerSelectionId; }
    public Long getParameterDefinitionId() { return parameterDefinitionId; }
    public void setParameterDefinitionId(Long parameterDefinitionId) { this.parameterDefinitionId = parameterDefinitionId; }
}
