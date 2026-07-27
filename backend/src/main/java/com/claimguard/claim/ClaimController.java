package com.claimguard.claim;

import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.CreateClaimRequest;
import com.claimguard.claim.dto.DocumentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailResponse create(@RequestBody(required = false) CreateClaimRequest request) {
        return service.create(request == null ? null : request.reference());
    }

    @GetMapping
    public List<ClaimSummaryResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ClaimDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return service.addDocument(id, file);
    }
}
