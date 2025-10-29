package org.mosesidowu.geolocation_core.service.rateLimiterService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.mosesidowu.geolocation_core.exception.RateLimitExceededException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 5; // Allowed requests per time window
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);
    private static final int MINUTE = 1;

    private Bucket createNewBucket() {
        // Allow 5 requests per minute
        Refill refill = Refill.greedy(MAX_REQUESTS, WINDOW_DURATION);
        Bandwidth limit = Bandwidth.classic(MAX_REQUESTS, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    public void consumeToken(String userId) {
        String key = (userId != null && !userId.isBlank()) ? userId : getClientIP();

        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket());
        if (!bucket.tryConsume(MINUTE)) {
            throw new RateLimitExceededException("Too many requests. Please wait a bit before trying again.");
        }
    }

    private String getClientIP() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null) ip = request.getRemoteAddr();
            return ip;
        }
        return "unknown";
    }
}
