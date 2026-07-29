package com.claimguard.fraud;

import com.claimguard.fraud.dto.RiskResponse;
import com.claimguard.fraud.dto.SignalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskLookup {

    private final ClaimRiskRepository risks;
    private final FraudSignalRepository signals;

    public RiskLookup(ClaimRiskRepository risks, FraudSignalRepository signals) {
        this.risks = risks;
        this.signals = signals;
    }

    @Transactional(readOnly = true)
    public RiskResponse forClaim(UUID claimId) {
        return risks.findById(claimId)
                .map(risk -> new RiskResponse(
                        risk.getScore(),
                        risk.getBand().name(),
                        risk.getSignalCount(),
                        risk.getAssessedAt(),
                        signals.findByClaimIdOrderByWeightDesc(claimId).stream().map(RiskLookup::toResponse).toList()))
                .orElseGet(RiskResponse::empty);
    }

    @Transactional(readOnly = true)
    public Map<UUID, RiskResponse> forClaims(List<UUID> claimIds) {
        Map<UUID, RiskResponse> byClaim = new HashMap<>();
        if (claimIds.isEmpty()) {
            return byClaim;
        }
        for (ClaimRisk risk : risks.findByClaimIdIn(claimIds)) {
            byClaim.put(risk.getClaimId(), new RiskResponse(
                    risk.getScore(),
                    risk.getBand().name(),
                    risk.getSignalCount(),
                    risk.getAssessedAt(),
                    List.of()));
        }
        return byClaim;
    }

    private static SignalResponse toResponse(FraudSignal signal) {
        return new SignalResponse(
                signal.getId(),
                signal.getDocumentId(),
                signal.getType().name(),
                signal.getSeverity().name(),
                signal.getWeight(),
                signal.getMessage(),
                signal.getDetails() == null ? Map.of() : signal.getDetails());
    }
}
