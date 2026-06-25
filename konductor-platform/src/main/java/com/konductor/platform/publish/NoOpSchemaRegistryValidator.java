package com.konductor.platform.publish;

import com.konductor.model.KonductorEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "konductor.schema-registry.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSchemaRegistryValidator implements SchemaRegistryValidator {

    @Override
    public void validate(KonductorEvent event) {
        // validation disabled — pass through
    }
}
