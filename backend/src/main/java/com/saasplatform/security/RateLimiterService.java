package com.saasplatform.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window token-bucket rate limiter.
 * Note: Suitable for single-instance deployments. For multi-instance distributed deployments,
 * replace the internal storage with Redis.
 */
@Slf4j
@Service
public class RateLimiterService {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxCapacity, int refillTokensPerMinute) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxCapacity, refillTokensPerMinute));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private Instant lastRefillTimestamp;

        public TokenBucket(int capacity, int refillTokensPerMinute) {
            this.capacity = capacity;
            this.refillRatePerSecond = (double) refillTokensPerMinute / 60.0;
            this.tokens = capacity;
            this.lastRefillTimestamp = Instant.now();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            Instant now = Instant.now();
            double secondsPassed = (double) (now.toEpochMilli() - lastRefillTimestamp.toEpochMilli()) / 1000.0;
            if (secondsPassed > 0) {
                tokens = Math.min(capacity, tokens + (secondsPassed * refillRatePerSecond));
                lastRefillTimestamp = now;
            }
        }
    }
}
