package com.claimguard.fhir;

import com.claimguard.fhir.dto.NhcxSubmissionResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/claims/{claimId}")
public class FhirController {

    private static final String FHIR_JSON = "application/fhir+json";

    private final FhirClaimService service;

    public FhirController(FhirClaimService service) {
        this.service = service;
    }

    @GetMapping(value = "/fhir", produces = FHIR_JSON)
    public ResponseEntity<String> bundle(@PathVariable UUID claimId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FHIR_JSON))
                .body(service.bundle(claimId));
    }

    @PostMapping("/nhcx")
    public NhcxSubmissionResponse submit(@PathVariable UUID claimId) {
        return service.submit(claimId);
    }
}
