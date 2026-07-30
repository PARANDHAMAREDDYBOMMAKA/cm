package com.claimguard.notify;

import com.claimguard.ai.JsonHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class NotifyConfig {

    @Bean
    @ConditionalOnMissingBean(Notifier.class)
    Notifier notifier(Environment environment,
            JsonMapper mapper,
            @Value("${RESEND_BASE_URL:https://api.resend.com}") String baseUrl,
            @Value("${NOTIFY_FROM:ClaimGuard <onboarding@resend.dev>}") String from,
            @Value("${NOTIFY_TIMEOUT_SECONDS:20}") long timeoutSeconds) {
        String apiKey = environment.getProperty("RESEND_API_KEY");
        List<String> recipients = split(environment.getProperty("NOTIFY_TO"));
        if (apiKey == null || apiKey.isBlank() || recipients.isEmpty()) {
            return new UnconfiguredNotifier();
        }
        JsonHttpClient http = new JsonHttpClient(baseUrl, Duration.ofSeconds(timeoutSeconds), mapper);
        return new ResendNotifier(http, apiKey, from, recipients);
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(entry -> !entry.isEmpty()).toList();
    }
}
