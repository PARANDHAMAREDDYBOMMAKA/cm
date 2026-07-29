package com.claimguard.claim;

import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.CreateClaimRequest;
import com.claimguard.claim.dto.DocumentResponse;
import com.claimguard.claim.dto.UpdateClaimRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
        return service.create(request != null ? request : new CreateClaimRequest(null, null, null));
    }

    @GetMapping
    public List<ClaimSummaryResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ClaimDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ClaimDetailResponse update(@PathVariable UUID id, @RequestBody UpdateClaimRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint) {
        return service.addDocument(id, file, deviceFingerprint);
    }

    @DeleteMapping("/{claimId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID claimId, @PathVariable UUID documentId) {
        service.deleteDocument(claimId, documentId);
    }

    @GetMapping("/{claimId}/documents/{documentId}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable UUID claimId, @PathVariable UUID documentId) {
        ClaimService.DocumentContent content = service.openDocument(claimId, documentId);
        MediaType mediaType = content.contentType() != null
                ? MediaType.parseMediaType(content.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.filename() + "\"")
                .contentLength(content.size())
                .body(content.resource());
    }
}
