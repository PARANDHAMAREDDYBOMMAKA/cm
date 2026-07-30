package com.claimguard.fhir;

import com.claimguard.fhir.dto.NhcxSubmissionResponse;

public interface NhcxGateway {

    boolean isAvailable();

    NhcxSubmissionResponse submit(String reference, String fhirBundle);
}
