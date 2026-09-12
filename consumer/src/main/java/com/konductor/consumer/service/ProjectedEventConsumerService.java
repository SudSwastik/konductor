package com.konductor.consumer.service;

import com.konductor.consumer.kafka.message.ProjectedEventMessage;

public interface ProjectedEventConsumerService {
    void consume(ProjectedEventMessage message);
}
