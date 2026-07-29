package com.claimguard.fraud;

import com.claimguard.ai.JsonHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class FraudConfig {

    @Bean
    EmbeddingProvider embeddingProvider(Environment environment,
            JsonMapper mapper,
            @Value("${CLOUDFLARE_BASE_URL:https://api.cloudflare.com}") String baseUrl,
            @Value("${EMBEDDING_MODEL:@cf/baai/bge-base-en-v1.5}") String model,
            @Value("${EMBEDDING_DIMENSIONS:768}") int dimensions,
            @Value("${EMBEDDING_TIMEOUT_SECONDS:60}") long timeoutSeconds) {
        String token = environment.getProperty("CLOUDFLARE_API_TOKEN");
        String account = environment.getProperty("CLOUDFLARE_ACCOUNT_ID");
        if (token == null || token.isBlank() || account == null || account.isBlank()) {
            return new UnconfiguredEmbeddingProvider();
        }
        JsonHttpClient http = new JsonHttpClient(baseUrl, Duration.ofSeconds(timeoutSeconds), mapper);
        return new CloudflareEmbeddingProvider(http, token, account, model, dimensions);
    }

    @Bean
    @ConditionalOnMissingBean(AiImageDetector.class)
    AiImageDetector unconfiguredAiImageDetector() {
        return new UnconfiguredAiImageDetector();
    }
}
