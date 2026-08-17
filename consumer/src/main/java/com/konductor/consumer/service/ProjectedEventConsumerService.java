package com.konductor.consumer.service;

import com.konductor.consumer.kafka.message.ConsumerAckMessage;
import com.konductor.consumer.kafka.message.ProjectedEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProjectedEventConsumerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectedEventConsumerService.class);
    private static final String ACKED = "ACKED";
    private static final String FAILED = "FAILED";

    private final ConsumerAckPublisher consumerAckPublisher;
    private final String consumerName;

    public ProjectedEventConsumerService(
            ConsumerAckPublisher consumerAckPublisher,
            @Value("${konductor.consumer.name}") String consumerName
    ) {
        this.consumerAckPublisher = consumerAckPublisher;
        this.consumerName = consumerName;
    }

    public void consume(ProjectedEventMessage message) {
        try {
            LOGGER.info("Received projected event {} for subscription {} with {} fields",
                    message.eventUid(),
                    message.subscriptionUid(),
                    message.data() == null ? 0 : message.data().size());
            consumerAckPublisher.publish(new ConsumerAckMessage(
                    message.eventUid(),
                    consumerName,
                    ACKED,
                    "Consumed successfully",
                    Instant.now()
            ));
        } catch (RuntimeException exception) {
            consumerAckPublisher.publish(new ConsumerAckMessage(
                    message.eventUid(),
                    consumerName,
                    FAILED,
                    exception.getMessage(),
                    Instant.now()
            ));
            throw exception;
        }
    }
}
