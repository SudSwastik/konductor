package com.konductor.projector.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konductor.projector.kafka.message.ConsumerAckMessage;
import com.konductor.projector.service.ConsumerAckService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumerAckListener {
    private final ObjectMapper objectMapper;
    private final ConsumerAckService consumerAckService;

    public ConsumerAckListener(ObjectMapper objectMapper, ConsumerAckService consumerAckService) {
        this.objectMapper = objectMapper;
        this.consumerAckService = consumerAckService;
    }

    @KafkaListener(
            topics = "${konductor.kafka.consumer-acks-topic}",
            groupId = "${spring.kafka.consumer.group-id}-acks",
            containerFactory = "ackKafkaListenerContainerFactory"
    )
    public void receive(String payload) throws JsonProcessingException {
        consumerAckService.apply(objectMapper.readValue(payload, ConsumerAckMessage.class));
    }
}
