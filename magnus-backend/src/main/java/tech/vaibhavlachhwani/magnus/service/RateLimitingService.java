package tech.vaibhavlachhwani.magnus.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in‑memory rate limiting service using Bucket4j.
 * Creates a token bucket per client identifier (e.g., IP address).
 */
@Service
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Return the bucket for the given client identifier, creating one if absent.
     */
    public Bucket resolveBucket(String clientId) {
        return buckets.computeIfAbsent(clientId, this::newBucket);
    }

    private Bucket newBucket(String clientId) {
        // Example: 10 requests per minute, burst capacity of 10.
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
