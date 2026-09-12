package com.konductor.consumer.service;

import com.konductor.consumer.kafka.message.ConsumerAckMessage;

public interface ConsumerAckPublisher {
    void publish(ConsumerAckMessage ack);
}
