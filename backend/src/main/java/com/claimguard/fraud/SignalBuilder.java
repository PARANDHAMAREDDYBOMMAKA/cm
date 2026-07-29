package com.claimguard.fraud;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SignalBuilder {

    private SignalBuilder() {
    }

    public static FraudSignal of(UUID claimId,
            UUID documentId,
            SignalType type,
            Severity severity,
            String message,
            Map<String, String> details) {
        FraudSignal signal = new FraudSignal();
        signal.setClaimId(claimId);
        signal.setDocumentId(documentId);
        signal.setType(type);
        signal.setSeverity(severity);
        signal.setWeight(severity.weight());
        signal.setMessage(message.length() <= 1000 ? message : message.substring(0, 1000));
        signal.setDetails(details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details));
        return signal;
    }
}
