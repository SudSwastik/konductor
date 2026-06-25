package com.konductor.platform.publish;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.konductor.model.KonductorEventBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublishController.class)
class PublishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublishService publishService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void publish_returns202_withEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(publishService.publish(any())).thenReturn(eventId);

        String body = objectMapper.writeValueAsString(
                KonductorEventBuilder.newEvent()
                        .eventType("order.created")
                        .sourceConfigId("order-service")
                        .payload(Map.of("orderId", "abc"))
                        .build());

        mockMvc.perform(post("/api/v1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()));
    }

    @Test
    void publish_returns400_whenServiceThrowsBadRequest() throws Exception {
        when(publishService.publish(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No schema registered"));

        String body = objectMapper.writeValueAsString(
                KonductorEventBuilder.newEvent()
                        .eventType("unknown.type")
                        .sourceConfigId("order-service")
                        .payload(Map.of("orderId", "abc"))
                        .build());

        mockMvc.perform(post("/api/v1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
