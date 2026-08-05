package com.claimguard.audit;

import com.claimguard.audit.dto.AuditEventResponse;
import com.claimguard.audit.dto.AuditVerificationResponse;
import com.claimguard.web.dto.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@Validated
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

    @GetMapping("/page")
    public PageResponse<AuditEventResponse> page(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size) {
        return lookup.page(page, size);
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
    public ResponseEntity<StreamingResponseBody> export() {
        StreamingResponseBody body = output -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            exporter.writeCsv(writer);
        };
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("claimguard-audit.csv", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
