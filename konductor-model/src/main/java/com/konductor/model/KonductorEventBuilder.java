package com.konductor.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KonductorEventBuilder {

    private String eventType;
    private String sourceConfigId;
    private String configType;
    private String schemaVersion;
    private String correlationId;
    private String traceId;
    private Map<String, Object> payload;
    private List<DataRef> dataRefs;

    private KonductorEventBuilder() {}

    public static KonductorEventBuilder newEvent() {
        return new KonductorEventBuilder();
    }

    public KonductorEventBuilder eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public KonductorEventBuilder sourceConfigId(String sourceConfigId) {
        this.sourceConfigId = sourceConfigId;
        return this;
    }

    public KonductorEventBuilder configType(String configType) {
        this.configType = configType;
        return this;
    }

    public KonductorEventBuilder schemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    public KonductorEventBuilder correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public KonductorEventBuilder traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public KonductorEventBuilder payload(Map<String, Object> payload) {
        this.payload = payload;
        return this;
    }

    public KonductorEventBuilder dataRefs(List<DataRef> dataRefs) {
        this.dataRefs = dataRefs;
        return this;
    }

    public KonductorEvent build() {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalStateException("eventType is required");
        }
        if (sourceConfigId == null || sourceConfigId.isBlank()) {
            throw new IllegalStateException("sourceConfigId is required");
        }
        if (payload == null) {
            throw new IllegalStateException("payload is required");
        }

        EventMetadata metadata = EventMetadata.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .eventTimestamp(Instant.now())
                .sourceConfigId(sourceConfigId)
                .configType(configType)
                .schemaVersion(schemaVersion)
                .correlationId(correlationId)
                .traceId(traceId)
                .build();

        return KonductorEvent.builder()
                .metadata(metadata)
                .payload(payload)
                .dataRefs(dataRefs)
                .build();
    }
}
