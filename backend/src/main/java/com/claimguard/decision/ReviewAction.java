package com.claimguard.decision;

public enum ReviewAction {
    APPROVE(DecisionOutcome.APPROVED, "approved after review"),
    FLAG(DecisionOutcome.FLAGGED, "held for further checks"),
    ESCALATE(DecisionOutcome.ESCALATED, "escalated to investigations");

    private final DecisionOutcome outcome;
    private final String description;

    ReviewAction(DecisionOutcome outcome, String description) {
        this.outcome = outcome;
        this.description = description;
    }

    public DecisionOutcome outcome() {
        return outcome;
    }

    public String description() {
        return description;
    }
}
