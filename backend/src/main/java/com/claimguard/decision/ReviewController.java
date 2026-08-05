package com.claimguard.decision;

import com.claimguard.decision.dto.DecisionResponse;
import com.claimguard.decision.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/claims/{claimId}")
public class ReviewController {

    private final ReviewService service;
    private final DecisionLookup lookup;

    public ReviewController(ReviewService service, DecisionLookup lookup) {
        this.service = service;
        this.lookup = lookup;
    }

    @GetMapping("/decision")
    public DecisionResponse decision(@PathVariable UUID claimId) {
        return lookup.forClaim(claimId);
    }

    @PostMapping("/review")
    public DecisionResponse review(@PathVariable UUID claimId, @RequestBody @Valid ReviewRequest request) {
        return service.review(claimId, request);
    }
}
