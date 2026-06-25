package com.konductor.producer;

import com.konductor.model.KonductorEvent;

public interface KonductorPublisher {
    void publish(KonductorEvent event);
}
