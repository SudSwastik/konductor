package com.konductor.projector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parameter_definition")
public class ParameterDefinition extends MutableAuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_type_id")
    private Short dataTypeId;

    private String name;
    private String description;

    @Column(name = "field_path")
    private String fieldPath;

    @Column(name = "is_required")
    private boolean required;

    public Long getId() { return id; }
    public Short getDataTypeId() { return dataTypeId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFieldPath() { return fieldPath; }
    public boolean isRequired() { return required; }
}
