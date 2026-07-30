package com.claimguard.fhir;

import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimDocument;
import com.claimguard.claim.ClaimNotFoundException;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.fhir.dto.NhcxSubmissionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FhirClaimService {

    private final ClaimRepository claims;
    private final DocumentExtractionRepository extractions;
    private final FhirClaimMapper mapper;
    private final NhcxGateway gateway;
    private final AuditService audit;

    public FhirClaimService(ClaimRepository claims,
            DocumentExtractionRepository extractions,
            FhirClaimMapper mapper,
            NhcxGateway gateway,
            AuditService audit) {
        this.claims = claims;
        this.extractions = extractions;
        this.mapper = mapper;
        this.gateway = gateway;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public String bundle(UUID claimId) {
        Claim claim = require(claimId);
        return mapper.toJson(claim, completedExtractions(claim));
    }

    @Transactional
    public NhcxSubmissionResponse submit(UUID claimId) {
        Claim claim = require(claimId);
        String bundle = mapper.toJson(claim, completedExtractions(claim));
        NhcxSubmissionResponse response = gateway.submit(claim.getReference(), bundle);
        audit.record(claim.getId(), claim.getReference(), AuditAction.NHCX_SUBMITTED,
                "An NHCX-shaped FHIR bundle was prepared for " + claim.getReference() + ".",
                Map.of("status", response.status(),
                        "delivered", String.valueOf(response.delivered()),
                        "correlationId", response.correlationId(),
                        "bundleBytes", String.valueOf(response.bundleBytes())));
        return response;
    }

    private List<DocumentExtraction> completedExtractions(Claim claim) {
        List<UUID> documentIds = claim.getDocuments().stream().map(ClaimDocument::getId).toList();
        if (documentIds.isEmpty()) {
            return List.of();
        }
        return extractions.findByDocumentIdIn(documentIds);
    }

    private Claim require(UUID claimId) {
        return claims.findById(claimId).orElseThrow(() -> new ClaimNotFoundException(claimId));
    }
}
