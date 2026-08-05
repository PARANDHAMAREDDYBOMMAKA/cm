package com.claimguard.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    private static final long IDLE_EVICTION_NANOS = 10L * 60 * 1_000_000_000L;
    private static final int EVICTION_THRESHOLD = 10_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final double capacity;
    private final double refillPerNano;

    public RateLimiter(int burst, int perMinute) {
        this.capacity = Math.max(1, burst);
        this.refillPerNano = Math.max(1, perMinute) / 60_000_000_000.0;
    }

    public boolean tryAcquire(String key) {
        if (buckets.size() > EVICTION_THRESHOLD) {
            evictIdle();
        }
        Bucket bucket = buckets.computeIfAbsent(key == null ? "anonymous" : key, ignored -> new Bucket(capacity));
        return bucket.tryAcquire(capacity, refillPerNano);
    }

    private void evictIdle() {
        long now = System.nanoTime();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastSeen() > IDLE_EVICTION_NANOS);
    }

    private static final class Bucket {

        private double tokens;
        private long updatedAt;

        private Bucket(double initialTokens) {
            this.tokens = initialTokens;
            this.updatedAt = System.nanoTime();
        }

        private synchronized long lastSeen() {
            return updatedAt;
        }

        private synchronized boolean tryAcquire(double capacity, double refillPerNano) {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - updatedAt) * refillPerNano);
            updatedAt = now;
            if (tokens < 1) {
                return false;
            }
            tokens -= 1;
            return true;
        }
    }
}
