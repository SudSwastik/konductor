package com.konductor.platform.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSubscriptionController.class)
class AdminSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    @Test
    void register_returns201_withPendingSubscription() throws Exception {
        SubscriptionEntity saved = pendingEntity(UUID.randomUUID(), "order.created", "sub-a");
        when(subscriptionRepository.save(any())).thenReturn(saved);

        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest();
        request.setSubscriberId("sub-a");
        request.setSubscriberName("sub-a-service");
        request.setEventType("order.created");
        request.setFieldPaths(List.of("$.orderId", "$.amount"));
        request.setOutputTopic("sub-a-events");

        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriberId").value("sub-a"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void approve_setsActiveTrue() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionEntity entity = pendingEntity(id, "order.created", "sub-b");
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(entity));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/admin/subscriptions/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());
    }

    @Test
    void reject_keepsActiveFlase() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionEntity entity = pendingEntity(id, "order.created", "sub-c");
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(entity));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/admin/subscriptions/{id}/reject", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void list_filtersByStatus() throws Exception {
        SubscriptionEntity active = activeEntity(UUID.randomUUID(), "order.created", "sub-d");
        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of(active));

        mockMvc.perform(get("/api/v1/admin/subscriptions").param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].subscriberId").value("sub-d"));
    }

    @Test
    void approve_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/admin/subscriptions/{id}/approve", id))
                .andExpect(status().isNotFound());
    }

    private SubscriptionEntity pendingEntity(UUID id, String eventType, String subscriberId) {
        return SubscriptionEntity.builder()
                .subscriberId(subscriberId)
                .subscriberName(subscriberId + "-service")
                .eventType(eventType)
                .fieldPaths(List.of("$.orderId", "$.amount"))
                .outputTopic(subscriberId + "-events")
                .active(false)
                .build();
    }

    private SubscriptionEntity activeEntity(UUID id, String eventType, String subscriberId) {
        return SubscriptionEntity.builder()
                .subscriberId(subscriberId)
                .subscriberName(subscriberId + "-service")
                .eventType(eventType)
                .fieldPaths(List.of("$.orderId"))
                .outputTopic(subscriberId + "-events")
                .active(true)
                .build();
    }
}
