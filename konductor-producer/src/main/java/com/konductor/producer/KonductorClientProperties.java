package com.konductor.producer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "konductor.client")
public class KonductorClientProperties {
    private String serverUrl;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
