package com.claimguard.notify;

import com.claimguard.decision.ClaimDecidedEvent;

public interface Notifier {

    boolean isAvailable();

    void notifyDecision(ClaimDecidedEvent event);
}
