package org.mosesidowu.geolocation_core.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private final Cache<String, AtomicInteger> requestCounts;

    private static final int MAX_REQUESTS = 5; // Allowed requests per time window
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    public RateLimiterService() {
        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(WINDOW_DURATION)
                .build();
    }

    public boolean tryConsume(String identifier) {
        AtomicInteger counter = requestCounts.get(identifier, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > MAX_REQUESTS) {
            return false; // Limit exceeded
        }
        return true; // Allowed
    }
}
