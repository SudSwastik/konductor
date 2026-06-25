package com.konductor.platform.publish;

import com.konductor.model.KonductorEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "konductor.schema-registry.enabled", havingValue = "true")
public class ConfluentSchemaRegistryValidator implements SchemaRegistryValidator {

    private final RestTemplate restTemplate;
    private final String schemaRegistryUrl;

    public ConfluentSchemaRegistryValidator(@Value("${konductor.schema-registry.url}") String schemaRegistryUrl) {
        this.restTemplate = new RestTemplate();
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    @Override
    public void validate(KonductorEvent event) {
        String subject = event.getMetadata().getEventType() + "-value";
        String url = schemaRegistryUrl + "/subjects/" + subject + "/versions/latest";
        try {
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No schema registered for event type: " + event.getMetadata().getEventType());
        }
    }
}
