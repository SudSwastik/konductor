package com.konductor.platform.dlq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDlqController.class)
class AdminDlqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeadLetterService deadLetterService;

    @Test
    void list_returnsPendingEvents() throws Exception {
        DeadLetterEventEntity entity = dlqEntity(UUID.randomUUID(), "PENDING", "order.created");
        when(deadLetterService.findByStatus("PENDING")).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/v1/admin/dlq").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].eventType").value("order.created"));
    }

    @Test
    void retry_callsDeadLetterService() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(deadLetterService).retry(id);

        mockMvc.perform(post("/api/v1/admin/dlq/{id}/retry", id))
                .andExpect(status().isOk());

        verify(deadLetterService).retry(id);
    }

    @Test
    void discard_callsDeadLetterService() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(deadLetterService).discard(id);

        mockMvc.perform(post("/api/v1/admin/dlq/{id}/discard", id))
                .andExpect(status().isOk());

        verify(deadLetterService).discard(id);
    }

    @Test
    void list_filtersbyEventType() throws Exception {
        DeadLetterEventEntity orderEvent = dlqEntity(UUID.randomUUID(), "PENDING", "order.created");
        DeadLetterEventEntity paymentEvent = dlqEntity(UUID.randomUUID(), "PENDING", "payment.processed");
        when(deadLetterService.findAll()).thenReturn(List.of(orderEvent, paymentEvent));

        mockMvc.perform(get("/api/v1/admin/dlq").param("eventType", "order.created"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("order.created"));
    }

    private DeadLetterEventEntity dlqEntity(UUID id, String status, String eventType) {
        return DeadLetterEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .originalEvent("{\"metadata\":{\"eventType\":\"" + eventType + "\"}}")
                .subscriberId("sub-a")
                .failureReason("Simulated failure")
                .status(status)
                .build();
    }
}
