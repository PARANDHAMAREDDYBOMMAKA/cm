package com.claimguard.extraction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    ExtractionQueue localExtractionQueue(TaskExecutor extractionTaskExecutor, ExtractionService service) {
        return new LocalExtractionQueue(extractionTaskExecutor, service);
    }
}
