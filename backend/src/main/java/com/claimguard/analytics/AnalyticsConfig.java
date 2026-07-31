package com.claimguard.analytics;

import com.claimguard.ai.JsonHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class AnalyticsConfig {

    @Bean
    @ConditionalOnMissingBean(Analytics.class)
    Analytics analytics(Environment environment,
            JsonMapper mapper,
            @Value("${POSTHOG_HOST:https://eu.i.posthog.com}") String host,
            @Value("${POSTHOG_TIMEOUT_SECONDS:10}") long timeoutSeconds) {
        String apiKey = environment.getProperty("POSTHOG_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return new UnconfiguredAnalytics();
        }
        return new PostHogAnalytics(new JsonHttpClient(host, Duration.ofSeconds(timeoutSeconds), mapper), apiKey);
    }
}
