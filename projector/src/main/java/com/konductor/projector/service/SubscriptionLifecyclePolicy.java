package com.konductor.projector.service;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class SubscriptionLifecyclePolicy {
    public static final String SCHEDULED = "SCHEDULED";
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public static final String ARCHIVED = "ARCHIVED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            SCHEDULED, Set.of(SCHEDULED, ACTIVE, INACTIVE, ARCHIVED),
            ACTIVE, Set.of(SCHEDULED, ACTIVE, INACTIVE, ARCHIVED),
            INACTIVE, Set.of(SCHEDULED, ACTIVE, INACTIVE, ARCHIVED),
            ARCHIVED, Set.of(INACTIVE, ARCHIVED)
    );

    public String initialStatus(LocalDate goLiveDate) {
        return goLiveDate.isAfter(LocalDate.now(ZoneOffset.UTC)) ? SCHEDULED : ACTIVE;
    }

    public void validateTransition(String currentStatus, String targetStatus) {
        Set<String> allowed = TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid subscription lifecycle transition: " + currentStatus + " -> " + targetStatus
            );
        }
    }

    public boolean delivers(String status) {
        return ACTIVE.equals(status);
    }
}
