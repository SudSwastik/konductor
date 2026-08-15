package com.konductor.projector.dto;

public record MasterDataResponse(
        Number id,
        String code,
        String name,
        String description
) {
}
