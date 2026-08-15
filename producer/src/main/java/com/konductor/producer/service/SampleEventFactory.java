package com.konductor.producer.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SampleEventFactory {
    private static final String SOURCE_SYSTEM = "konductor-producer";

    public Map<String, Object> bodyFor(String triggerType) {
        String orderId = "ord_" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", Map.of(
                        "triggerType", triggerType,
                        "occurredAt", Instant.now().toString(),
                        "sourceSystem", SOURCE_SYSTEM,
                        "correlationId", "corr_" + UUID.randomUUID().toString().substring(0, 8)
                ));
        body.put("order", Map.of(
                        "orderId", orderId,
                        "orderNumber", "KO-" + LocalDate.now().getYear() + "-" + orderId.substring(4).toUpperCase(),
                        "status", statusFor(triggerType),
                        "createdAt", Instant.now().minusSeconds(3600).toString(),
                        "updatedAt", Instant.now().toString(),
                        "total", Map.of("amount", new BigDecimal("149.99"), "currency", "USD"),
                        "subtotal", Map.of("amount", new BigDecimal("129.99")),
                        "tax", Map.of("amount", new BigDecimal("10.00")),
                        "discount", Map.of("amount", new BigDecimal("0.00")),
                        "shipping", Map.of("amount", new BigDecimal("10.00"))
                ));
        body.put("customer", Map.of(
                        "customerId", "cus_" + UUID.randomUUID().toString().substring(0, 8),
                        "externalId", "crm-" + UUID.randomUUID().toString().substring(0, 8),
                        "firstName", "Asha",
                        "lastName", "Rao",
                        "email", "asha.rao@example.com",
                        "phone", "+15551234567",
                        "marketingOptIn", true
                ));
        body.put("items", List.of(
                        Map.of(
                                "sku", "SKU-1001",
                                "name", "Konductor Starter Kit",
                                "quantity", 1,
                                "unitPrice", Map.of("amount", new BigDecimal("129.99"), "currency", "USD")
                        )
                ));
        body.put("payment", Map.of(
                        "paymentId", "pay_" + UUID.randomUUID().toString().substring(0, 8),
                        "method", "CARD",
                        "status", paymentStatusFor(triggerType),
                        "authorizedAmount", Map.of("amount", new BigDecimal("149.99"), "currency", "USD")
                ));
        body.put("billingAddress", Map.of("country", "US", "postalCode", "10001"));
        body.put("shippingAddress", Map.of("country", "US", "postalCode", "10001"));
        body.put("shipment", Map.of(
                        "carrier", "UPS",
                        "trackingNumber", "1Z" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(),
                        "estimatedDeliveryAt", Instant.now().plusSeconds(259200).toString()
                ));
        body.put("channel", Map.of("name", "ONLINE", "storeId", "store-us"));
        body.put("promotions", List.of(Map.of("code", "WELCOME10")));
        body.put("metadata", Map.of("schema", "commerce-master-v1"));
        return body;
    }

    private String statusFor(String triggerType) {
        return switch (triggerType) {
            case "ORDER_CANCELLED" -> "CANCELLED";
            case "SHIPMENT_UPDATED" -> "SHIPPED";
            default -> "OPEN";
        };
    }

    private String paymentStatusFor(String triggerType) {
        return switch (triggerType) {
            case "PAYMENT_UPDATED" -> "CAPTURED";
            case "ORDER_CANCELLED" -> "VOIDED";
            default -> "AUTHORIZED";
        };
    }
}
