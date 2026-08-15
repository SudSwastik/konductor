package com.konductor.projector.dto;

import java.util.List;

public record EventTriggerSelectionResponse(
        Short eventTriggerTypeId,
        List<Long> parameterDefinitionIds
) {
}
