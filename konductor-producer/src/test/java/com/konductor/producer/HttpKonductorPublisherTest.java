package com.konductor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.konductor.model.KonductorEvent;
import com.konductor.model.KonductorEventBuilder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpKonductorPublisherTest {

    private MockWebServer server;
    private HttpKonductorPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        KonductorClientProperties properties = new KonductorClientProperties();
        properties.setServerUrl(server.url("").toString().replaceAll("/$", ""));
        publisher = new HttpKonductorPublisher(properties);

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void publish_sendsCorrectJsonBodyAndHeaders() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));

        KonductorEvent event = KonductorEventBuilder.newEvent()
                .eventType("order.created")
                .sourceConfigId("order-service")
                .payload(Map.of("orderId", "abc", "amount", 99.5))
                .build();

        publisher.publish(event);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/publish");
        assertThat(request.getHeader("Content-Type")).contains("application/json");

        KonductorEvent received = objectMapper.readValue(request.getBody().readUtf8(), KonductorEvent.class);
        assertThat(received.getMetadata().getEventType()).isEqualTo("order.created");
        assertThat(received.getMetadata().getSourceConfigId()).isEqualTo("order-service");
        assertThat(received.getPayload()).containsKey("orderId");
    }

    @Test
    void publish_throwsOnClientError() {
        server.enqueue(new MockResponse().setResponseCode(400).setBody("Bad Request"));

        assertThatThrownBy(() -> publisher.publish(buildEvent()))
                .isInstanceOf(KonductorPublishException.class)
                .hasMessageContaining("400");
    }

    @Test
    void publish_throwsOnServerError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        assertThatThrownBy(() -> publisher.publish(buildEvent()))
                .isInstanceOf(KonductorPublishException.class)
                .hasMessageContaining("500");
    }

    @Test
    void publish_throwsOnConnectionFailure() throws IOException {
        server.shutdown();

        assertThatThrownBy(() -> publisher.publish(buildEvent()))
                .isInstanceOf(KonductorPublishException.class);
    }

    private KonductorEvent buildEvent() {
        return KonductorEventBuilder.newEvent()
                .eventType("order.created")
                .sourceConfigId("order-service")
                .payload(Map.of("orderId", "abc"))
                .build();
    }
}
