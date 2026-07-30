package com.claimguard.decision;

import com.claimguard.audit.Actors;
import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimNotFoundException;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.decision.dto.DecisionResponse;
import com.claimguard.decision.dto.ReviewRequest;
import com.claimguard.support.Values;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReviewService {

    private final ClaimRepository claims;
    private final ClaimDecisionRepository decisions;
    private final DecisionLookup lookup;
    private final AuditService audit;

    public ReviewService(ClaimRepository claims,
            ClaimDecisionRepository decisions,
            DecisionLookup lookup,
            AuditService audit) {
        this.claims = claims;
        this.decisions = decisions;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public DecisionResponse review(UUID claimId, ReviewRequest request) {
        Claim claim = claims.findById(claimId).orElseThrow(() -> new ClaimNotFoundException(claimId));
        ReviewAction action = parseAction(request.action());
        String note = Values.truncate(request.note(), 2000);
        String actor = Actors.current();

        ClaimDecision decision = decisions.findById(claimId).orElseGet(ClaimDecision::new);
        List<String> reasons = new ArrayList<>();
        reasons.add("Reviewed by " + actor + " and " + action.description() + ".");
        if (note != null) {
            reasons.add(note);
        }

        decision.setClaimId(claimId);
        decision.setOutcome(action.outcome());
        decision.setAutomatic(false);
        decision.setDecidedBy(actor);
        decision.setNote(note);
        decision.setReasons(reasons);
        decision.setDecidedAt(Instant.now());
        decisions.save(decision);

        claim.setStatus(action.outcome().status());
        claims.save(claim);

        Map<String, String> details = new LinkedHashMap<>();
        details.put("action", action.name());
        details.put("outcome", action.outcome().name());
        if (note != null) {
            details.put("note", note);
        }
        audit.record(claimId, claim.getReference(), AuditAction.REVIEW_RECORDED,
                "Claim " + action.description() + " by " + actor + ".", details);

        return lookup.forClaim(claimId);
    }

    private static ReviewAction parseAction(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review action is required");
        }
        try {
            return ReviewAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review action: " + value);
        }
    }
}
