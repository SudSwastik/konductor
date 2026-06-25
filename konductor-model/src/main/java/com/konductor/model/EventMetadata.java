package com.konductor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetadata {
    private UUID eventId;
    private String eventType;
    private Instant eventTimestamp;
    private String sourceConfigId;
    private String configType;
    private String schemaVersion;
    private String correlationId;
    private String traceId;
}
