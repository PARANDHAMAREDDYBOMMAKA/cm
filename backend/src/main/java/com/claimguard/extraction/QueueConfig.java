package com.claimguard.extraction;

import com.claimguard.ai.JsonHttpClient;
import com.claimguard.upstash.QStashClient;
import com.claimguard.upstash.QStashVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class QueueConfig {

    @Bean(name = "extractionTaskExecutor", destroyMethod = "shutdown")
    TaskExecutor extractionTaskExecutor(
            @Value("${EXTRACTION_WORKERS:4}") int workers,
            @Value("${EXTRACTION_QUEUE_CAPACITY:200}") int capacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(capacity);
        executor.setThreadNamePrefix("extraction-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(ExtractionQueue.class)
    ExtractionQueue extractionQueue(Environment environment,
            JsonMapper mapper,
            QStashVerifier verifier,
            TaskExecutor extractionTaskExecutor,
            ExtractionService service,
            @Value("${QSTASH_URL:https://qstash.upstash.io}") String qstashUrl,
            @Value("${QSTASH_TIMEOUT_SECONDS:10}") long timeoutSeconds,
            @Value("${QSTASH_RETRIES:3}") int retries,
            @Value("${EXTRACTION_WORKERS:4}") int workers) {
        String token = environment.getProperty("QSTASH_TOKEN");
        if (token == null || token.isBlank() || !verifier.isConfigured()) {
            return new LocalExtractionQueue(extractionTaskExecutor, service);
        }
        JsonHttpClient http = new JsonHttpClient(qstashUrl, Duration.ofSeconds(timeoutSeconds), mapper);
        QStashClient client = new QStashClient(http, qstashUrl, token);
        return new QStashExtractionQueue(client, verifier.destination(), workers, retries);
    }
}
