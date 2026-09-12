package com.konductor.projector.dto;

public record ErrorItem(
        String field,
        String reason
) {
}
