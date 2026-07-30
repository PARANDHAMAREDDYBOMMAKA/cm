package com.claimguard.fhir;

import com.claimguard.fhir.dto.NhcxSubmissionResponse;

import java.time.Instant;
import java.util.UUID;

public class StubNhcxGateway implements NhcxGateway {

    private final String participantCode;

    public StubNhcxGateway(String participantCode) {
        this.participantCode = participantCode;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public NhcxSubmissionResponse submit(String reference, String fhirBundle) {
        return new NhcxSubmissionResponse(
                UUID.randomUUID().toString(),
                reference,
                participantCode,
                "PREPARED",
                false,
                fhirBundle.length(),
                Instant.now(),
                "The bundle is NHCX-shaped and ready. No exchange endpoint is configured, so nothing was sent.");
    }
}
