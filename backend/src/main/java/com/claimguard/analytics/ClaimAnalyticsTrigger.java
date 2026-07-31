package com.claimguard.analytics;

import com.claimguard.claim.ClaimCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class ClaimAnalyticsTrigger {

    private static final Logger log = LoggerFactory.getLogger(ClaimAnalyticsTrigger.class);

    private final TaskExecutor executor;
    private final Analytics analytics;

    public ClaimAnalyticsTrigger(TaskExecutor extractionTaskExecutor, Analytics analytics) {
        this.executor = extractionTaskExecutor;
        this.analytics = analytics;
    }

    @TransactionalEventListener
    public void onClaimCreated(ClaimCreatedEvent event) {
        if (!analytics.isAvailable()) {
            return;
        }
        executor.execute(() -> {
            try {
                analytics.capture("claim_created", event.claimId().toString(),
                        Map.of("reference", event.reference()));
            } catch (RuntimeException exception) {
                log.warn("Could not send analytics for claim {}: {}", event.reference(), exception.getMessage());
            }
        });
    }
}
