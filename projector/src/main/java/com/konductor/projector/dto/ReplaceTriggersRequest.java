package com.konductor.projector.dto;

import java.util.List;

public record ReplaceTriggersRequest(
        List<Short> eventTriggerTypeIds
) {
}
