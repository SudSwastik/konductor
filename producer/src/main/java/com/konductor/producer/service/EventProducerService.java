package com.konductor.producer.service;

import com.konductor.producer.dto.PublishEventRequest;
import com.konductor.producer.dto.PublishEventResponse;

public interface EventProducerService {
    PublishEventResponse publishSample(String triggerType);
    PublishEventResponse publish(PublishEventRequest request);
}
