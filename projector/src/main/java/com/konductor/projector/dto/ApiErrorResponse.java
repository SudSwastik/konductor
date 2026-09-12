package com.konductor.projector.dto;

import java.util.List;

public record ApiErrorResponse(
        String errorMessage,
        List<ErrorItem> errors
) {
}
