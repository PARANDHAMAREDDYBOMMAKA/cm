package com.claimguard.decision;

import com.claimguard.decision.dto.DecisionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DecisionLookup {

    private final ClaimDecisionRepository decisions;

    public DecisionLookup(ClaimDecisionRepository decisions) {
        this.decisions = decisions;
    }

    @Transactional(readOnly = true)
    public DecisionResponse forClaim(UUID claimId) {
        return decisions.findById(claimId).map(DecisionLookup::toResponse).orElseGet(DecisionResponse::pending);
    }

    @Transactional(readOnly = true)
    public Map<UUID, DecisionResponse> forClaims(List<UUID> claimIds) {
        Map<UUID, DecisionResponse> byClaim = new HashMap<>();
        if (claimIds.isEmpty()) {
            return byClaim;
        }
        for (ClaimDecision decision : decisions.findByClaimIdIn(claimIds)) {
            byClaim.put(decision.getClaimId(), toResponse(decision));
        }
        return byClaim;
    }

    private static DecisionResponse toResponse(ClaimDecision decision) {
        return new DecisionResponse(
                decision.getOutcome().name(),
                decision.isAutomatic(),
                decision.getDecidedBy(),
                decision.getNote(),
                decision.getReasons() == null ? List.of() : List.copyOf(decision.getReasons()),
                decision.getDecidedAt());
    }
}
