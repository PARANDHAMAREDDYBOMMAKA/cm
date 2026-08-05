package com.claimguard.claim;

import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimStatusResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.CreateClaimRequest;
import com.claimguard.claim.dto.DocumentResponse;
import com.claimguard.claim.dto.UpdateClaimRequest;
import com.claimguard.web.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@Validated
public class ClaimController {

    private static final String NOSNIFF = "X-Content-Type-Options";
    private static final String CSP = "Content-Security-Policy";
    private static final String DOCUMENT_CSP = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDetailResponse create(@RequestBody(required = false) @Valid CreateClaimRequest request) {
        return service.create(request != null ? request : new CreateClaimRequest(null, null, null));
    }

    @GetMapping
    public PageResponse<ClaimSummaryResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.list(page, size);
    }

    @GetMapping("/queue/review")
    public PageResponse<ClaimSummaryResponse> reviewQueue(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.reviewQueue(page, size);
    }

    @GetMapping("/{id}")
    public ClaimDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/status")
    public ClaimStatusResponse status(@PathVariable UUID id) {
        return service.status(id);
    }

    @PutMapping("/{id}")
    public ClaimDetailResponse update(@PathVariable UUID id, @RequestBody @Valid UpdateClaimRequest request) {
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
        String contentType = content.contentType();
        ContentDisposition disposition = ContentDisposition
                .builder(UploadPolicy.isInlineSafe(contentType) ? "inline" : "attachment")
                .filename(content.filename() != null ? content.filename() : "document", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(NOSNIFF, "nosniff")
                .header(CSP, DOCUMENT_CSP)
                .contentLength(content.size())
                .body(content.resource());
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
