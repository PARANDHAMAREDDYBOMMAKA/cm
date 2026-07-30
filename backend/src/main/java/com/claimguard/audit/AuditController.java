package com.claimguard.audit;

import com.claimguard.audit.dto.AuditEventResponse;
import com.claimguard.audit.dto.AuditVerificationResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final int MAX_RECENT = 500;

    private final AuditLookup lookup;
    private final AuditExporter exporter;

    public AuditController(AuditLookup lookup, AuditExporter exporter) {
        this.lookup = lookup;
        this.exporter = exporter;
    }

    @GetMapping
    public List<AuditEventResponse> recent(@RequestParam(defaultValue = "100") int limit) {
        return lookup.recent(Math.min(Math.max(limit, 1), MAX_RECENT));
    }

    @GetMapping("/claims/{claimId}")
    public List<AuditEventResponse> forClaim(@PathVariable UUID claimId) {
        return lookup.forClaim(claimId);
    }

    @GetMapping("/verify")
    public AuditVerificationResponse verify() {
        return lookup.verify();
    }

    @GetMapping("/export")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claimguard-audit.csv\"")
                .body(exporter.toCsv());
    }
}
