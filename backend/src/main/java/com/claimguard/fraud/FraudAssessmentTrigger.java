package com.claimguard.fraud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FraudAssessmentTrigger {

    private static final Logger log = LoggerFactory.getLogger(FraudAssessmentTrigger.class);

    private final TaskExecutor executor;
    private final FraudAssessmentService service;

    public FraudAssessmentTrigger(TaskExecutor extractionTaskExecutor, FraudAssessmentService service) {
        this.executor = extractionTaskExecutor;
        this.service = service;
    }

    @TransactionalEventListener
    public void onAssessmentRequested(ClaimAssessmentRequestedEvent event) {
        executor.execute(() -> {
            try {
                service.assess(event.claimId());
            } catch (RuntimeException exception) {
                log.warn("Fraud assessment failed for claim {}", event.claimId(), exception);
            }
        });
    }
}
