package com.konductor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KonductorEvent {
    private EventMetadata metadata;
    private Map<String, Object> payload;
    private List<DataRef> dataRefs;
}
