package com.claimguard.support;

import com.claimguard.upstash.UpstashRedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String TOKEN_BUCKET = """
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local state = redis.call('HMGET', KEYS[1], 'tokens', 'at')
            local tokens = tonumber(state[1]) or capacity
            local at = tonumber(state[2]) or now
            tokens = math.min(capacity, tokens + math.max(0, now - at) * refill)
            local allowed = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            end
            redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'at', tostring(now))
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return allowed
            """;

    private final UpstashRedis redis;
    private final String prefix;
    private final double capacity;
    private final double refillPerMilli;
    private final long idleMillis;
    private final RateLimiter fallback;

    public RedisRateLimiter(UpstashRedis redis, String prefix, int burst, int perMinute) {
        this.redis = redis;
        this.prefix = prefix;
        this.capacity = Math.max(1, burst);
        this.refillPerMilli = Math.max(1, perMinute) / 60_000.0;
        this.idleMillis = (long) Math.ceil(capacity / refillPerMilli) + 60_000;
        this.fallback = new LocalRateLimiter(burst, perMinute);
    }

    @Override
    public boolean tryAcquire(String key) {
        String subject = key == null ? "anonymous" : key;
        try {
            return redis.eval(TOKEN_BUCKET,
                    List.of(prefix + subject),
                    List.of(String.valueOf(capacity),
                            String.valueOf(refillPerMilli),
                            String.valueOf(System.currentTimeMillis()),
                            String.valueOf(idleMillis)))
                    .asLong() == 1;
        } catch (RuntimeException exception) {
            log.warn("Redis rate limit unavailable, limiting in memory: {}", exception.getMessage());
            return fallback.tryAcquire(subject);
        }
    }
}
