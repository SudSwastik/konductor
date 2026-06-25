package com.konductor.platform.publish;

import com.konductor.model.KonductorEvent;

public interface SchemaRegistryValidator {
    void validate(KonductorEvent event);
}
