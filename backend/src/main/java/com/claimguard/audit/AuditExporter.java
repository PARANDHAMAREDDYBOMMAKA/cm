package com.claimguard.audit;

import com.claimguard.audit.dto.AuditEventResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AuditExporter {

    private static final String HEADER =
            "seq,recorded_at,claim_reference,actor,action,summary,details,previous_hash,hash\n";

    private final AuditLookup lookup;
    private final AuditService audit;

    public AuditExporter(AuditLookup lookup, AuditService audit) {
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public String toCsv() {
        List<AuditEventResponse> events = lookup.all();
        StringBuilder csv = new StringBuilder(HEADER);
        for (AuditEventResponse event : events) {
            csv.append(event.seq()).append(',')
                    .append(quote(String.valueOf(event.createdAt()))).append(',')
                    .append(quote(event.claimReference())).append(',')
                    .append(quote(event.actor())).append(',')
                    .append(quote(event.action())).append(',')
                    .append(quote(event.summary())).append(',')
                    .append(quote(flatten(event.details()))).append(',')
                    .append(quote(event.previousHash())).append(',')
                    .append(quote(event.hash())).append('\n');
        }
        audit.record(null, null, AuditAction.AUDIT_EXPORTED,
                "The audit trail was exported as CSV.",
                Map.of("entries", String.valueOf(events.size())));
        return csv.toString();
    }

    private static String flatten(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        details.forEach((key, value) -> text.append(key).append('=').append(value).append("; "));
        return text.toString().trim();
    }

    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
