package com.claimguard.notify;

import com.claimguard.analytics.Analytics;
import com.claimguard.decision.ClaimDecidedEvent;
import com.claimguard.decision.DecisionOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class DecisionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DecisionDispatcher.class);

    private static final Set<DecisionOutcome> ALERTING = EnumSet.of(
            DecisionOutcome.NEEDS_REVIEW, DecisionOutcome.FLAGGED, DecisionOutcome.ESCALATED);

    private final TaskExecutor executor;
    private final Notifier notifier;
    private final Analytics analytics;

    public DecisionDispatcher(TaskExecutor extractionTaskExecutor, Notifier notifier, Analytics analytics) {
        this.executor = extractionTaskExecutor;
        this.notifier = notifier;
        this.analytics = analytics;
    }

    @TransactionalEventListener
    public void onClaimDecided(ClaimDecidedEvent event) {
        executor.execute(() -> {
            capture(event);
            alert(event);
        });
    }

    private void capture(ClaimDecidedEvent event) {
        if (!analytics.isAvailable()) {
            return;
        }
        try {
            analytics.capture("claim_decided", event.claimId().toString(), Map.of(
                    "reference", event.reference(),
                    "outcome", event.outcome().name(),
                    "automatic", event.automatic(),
                    "riskScore", event.riskScore()));
        } catch (RuntimeException exception) {
            log.warn("Could not send analytics for claim {}: {}", event.reference(), exception.getMessage());
        }
    }

    private void alert(ClaimDecidedEvent event) {
        if (!notifier.isAvailable() || !ALERTING.contains(event.outcome())) {
            return;
        }
        try {
            notifier.notifyDecision(event);
        } catch (RuntimeException exception) {
            log.warn("Could not notify on claim {}: {}", event.reference(), exception.getMessage());
        }
    }
}
