package com.konductor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.konductor.model.KonductorEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpKonductorPublisher implements KonductorPublisher {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String publishUrl;
    private final Duration readTimeout;

    public HttpKonductorPublisher(KonductorClientProperties properties) {
        this.publishUrl = properties.getServerUrl() + "/api/v1/publish";
        this.readTimeout = Duration.ofMillis(properties.getReadTimeoutMs());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void publish(KonductorEvent event) {
        try {
            String body = objectMapper.writeValueAsString(event);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(publishUrl))
                    .header("Content-Type", "application/json")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new KonductorPublishException(
                        "Publish failed with HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (KonductorPublishException e) {
            throw e;
        } catch (Exception e) {
            throw new KonductorPublishException("Failed to publish event: " + e.getMessage(), e);
        }
    }
}
