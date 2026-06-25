package com.konductor.producer;

public class KonductorPublishException extends RuntimeException {

    public KonductorPublishException(String message) {
        super(message);
    }

    public KonductorPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
