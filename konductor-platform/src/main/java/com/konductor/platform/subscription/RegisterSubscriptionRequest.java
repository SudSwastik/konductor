package com.konductor.platform.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RegisterSubscriptionRequest {

    @NotBlank
    private String subscriberId;

    @NotBlank
    private String subscriberName;

    @NotBlank
    private String eventType;

    @NotEmpty
    private List<String> fieldPaths;

    @NotBlank
    private String outputTopic;
}
