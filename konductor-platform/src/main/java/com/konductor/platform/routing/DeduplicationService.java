package com.konductor.platform.routing;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class DeduplicationService {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public DeduplicationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isDuplicate(String subscriberId, UUID eventId) {
        String key = "dedup:" + subscriberId + ":" + eventId;
        Boolean isNew = redis.opsForValue().setIfAbsent(key, "1", TTL);
        return Boolean.FALSE.equals(isNew);
    }
}
