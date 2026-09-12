package com.konductor.projector.service;

import com.konductor.projector.kafka.message.ProducerEventMessage;

public interface SourceEventProjectionService {
    void project(ProducerEventMessage message);
}
