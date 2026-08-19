package com.claimguard.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "R2_BUCKET")
    StorageService r2StorageService(
            @Value("${R2_ENDPOINT}") String endpoint,
            @Value("${R2_BUCKET}") String bucket,
            @Value("${R2_ACCESS_KEY_ID}") String accessKeyId,
            @Value("${R2_SECRET_ACCESS_KEY}") String secretAccessKey) {
        return new R2StorageService(endpoint, bucket, accessKeyId, secretAccessKey);
    }

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    StorageService unconfiguredStorageService() {
        return new UnconfiguredStorageService();
    }
}
