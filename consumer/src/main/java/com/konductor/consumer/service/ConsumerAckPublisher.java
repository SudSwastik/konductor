package com.konductor.consumer.service;

import com.konductor.consumer.kafka.message.ConsumerAckMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsumerAckPublisher {
    private final KafkaTemplate<String, ConsumerAckMessage> kafkaTemplate;
    private final String ackTopic;

    public ConsumerAckPublisher(
            KafkaTemplate<String, ConsumerAckMessage> kafkaTemplate,
            @Value("${konductor.kafka.consumer-acks-topic}") String ackTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.ackTopic = ackTopic;
    }

    public void publish(ConsumerAckMessage ack) {
        kafkaTemplate.send(ackTopic, ack.eventUid(), ack);
    }
}
