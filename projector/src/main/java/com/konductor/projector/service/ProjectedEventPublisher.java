package com.konductor.projector.service;

import com.konductor.projector.kafka.message.ProjectedEventMessage;

public interface ProjectedEventPublisher {
    void publish(String topic, ProjectedEventMessage message);
}
