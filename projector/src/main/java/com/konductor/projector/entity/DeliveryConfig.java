package com.konductor.projector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_config")
public class DeliveryConfig extends MutableAuditFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "delivery_type")
    private String deliveryType;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "auth_type")
    private String authType = "NONE";

    @Column(name = "auth_header_name")
    private String authHeaderName;

    @Column(name = "auth_secret_ref")
    private String authSecretRef;

    @Column(name = "timeout_seconds")
    private int timeoutSeconds = 30;

    @Column(name = "max_retry_count")
    private int maxRetryCount = 3;

    @Column(name = "retry_backoff_seconds")
    private int retryBackoffSeconds = 60;

    public Long getId() { return id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getAuthHeaderName() { return authHeaderName; }
    public void setAuthHeaderName(String authHeaderName) { this.authHeaderName = authHeaderName; }
    public String getAuthSecretRef() { return authSecretRef; }
    public void setAuthSecretRef(String authSecretRef) { this.authSecretRef = authSecretRef; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public int getRetryBackoffSeconds() { return retryBackoffSeconds; }
    public void setRetryBackoffSeconds(int retryBackoffSeconds) { this.retryBackoffSeconds = retryBackoffSeconds; }
}
