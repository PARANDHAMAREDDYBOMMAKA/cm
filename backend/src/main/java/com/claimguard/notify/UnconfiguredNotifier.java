package com.claimguard.notify;

import com.claimguard.decision.ClaimDecidedEvent;

public class UnconfiguredNotifier implements Notifier {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void notifyDecision(ClaimDecidedEvent event) {
    }
}
