package com.claimguard.claim;

import com.claimguard.ai.JsonHttpClient;
import com.claimguard.support.LocalRateLimiter;
import com.claimguard.support.RateLimiter;
import com.claimguard.support.RedisRateLimiter;
import com.claimguard.upstash.UpstashRedis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class UploadConfig {

    @Bean
    RateLimiter uploadRateLimiter(Environment environment,
            JsonMapper mapper,
            @Value("${UPLOAD_BURST:10}") int burst,
            @Value("${UPLOAD_PER_MINUTE:30}") int perMinute,
            @Value("${UPSTASH_REDIS_TIMEOUT_SECONDS:3}") long timeoutSeconds) {
        String url = environment.getProperty("UPSTASH_REDIS_REST_URL");
        String token = environment.getProperty("UPSTASH_REDIS_REST_TOKEN");
        if (url == null || url.isBlank() || token == null || token.isBlank()) {
            return new LocalRateLimiter(burst, perMinute);
        }
        JsonHttpClient http = new JsonHttpClient(url, Duration.ofSeconds(timeoutSeconds), mapper);
        return new RedisRateLimiter(new UpstashRedis(http, token), "claimguard:uploads:", burst, perMinute);
    }
}
