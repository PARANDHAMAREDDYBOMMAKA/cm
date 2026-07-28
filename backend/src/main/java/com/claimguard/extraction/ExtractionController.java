package com.claimguard.extraction;

import com.claimguard.extraction.dto.ExtractionResponse;
import com.claimguard.extraction.dto.UpdateExtractionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims/{claimId}/documents/{documentId}/extraction")
public class ExtractionController {

    private final ExtractionStore store;
    private final ExtractionLauncher launcher;

    public ExtractionController(ExtractionStore store, ExtractionLauncher launcher) {
        this.store = store;
        this.launcher = launcher;
    }

    @GetMapping
    public ExtractionResponse get(@PathVariable UUID claimId, @PathVariable UUID documentId) {
        return store.requireInClaim(claimId, documentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExtractionResponse rerun(@PathVariable UUID claimId, @PathVariable UUID documentId) {
        store.requireDocumentInClaim(claimId, documentId);
        return launcher.launch(documentId, true);
    }

    @PatchMapping
    public ExtractionResponse update(@PathVariable UUID claimId,
            @PathVariable UUID documentId,
            @RequestBody UpdateExtractionRequest request) {
        Map<String, String> fields = request.fields();
        if (fields == null || fields.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }
        return store.applyEdits(claimId, documentId, fields);
    }
}
