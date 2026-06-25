package com.konductor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRef {
    private String uri;
    private String contentType;
    private String checksum;
    private Long sizeBytes;
}
