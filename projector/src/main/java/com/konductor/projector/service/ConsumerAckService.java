package com.konductor.projector.service;

import com.konductor.projector.kafka.message.ConsumerAckMessage;

public interface ConsumerAckService {
    void apply(ConsumerAckMessage ack);
}
