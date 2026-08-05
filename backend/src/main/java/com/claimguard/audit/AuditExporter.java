package com.claimguard.audit;

import com.claimguard.audit.dto.AuditEventResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditExporter {

    private static final String HEADER =
            "seq,recorded_at,claim_reference,actor,action,summary,details,previous_hash,hash\n";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private final AuditLookup lookup;
    private final AuditService audit;

    public AuditExporter(AuditLookup lookup, AuditService audit) {
        this.lookup = lookup;
        this.audit = audit;
    }

    public void writeCsv(Writer writer) throws IOException {
        writer.write(HEADER);
        AtomicLong exported = new AtomicLong();
        try {
            lookup.stream(event -> {
                write(writer, event);
                exported.incrementAndGet();
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
        writer.flush();
        audit.record(null, null, AuditAction.AUDIT_EXPORTED,
                "The audit trail was exported as CSV.",
                Map.of("entries", String.valueOf(exported.get())));
    }

    private static void write(Writer writer, AuditEventResponse event) {
        String row = new StringBuilder()
                .append(event.seq()).append(',')
                .append(quote(String.valueOf(event.createdAt()))).append(',')
                .append(quote(event.claimReference())).append(',')
                .append(quote(event.actor())).append(',')
                .append(quote(event.action())).append(',')
                .append(quote(event.summary())).append(',')
                .append(quote(flatten(event.details()))).append(',')
                .append(quote(event.previousHash())).append(',')
                .append(quote(event.hash())).append('\n')
                .toString();
        try {
            writer.write(row);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
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
        String safe = value;
        if (!safe.isEmpty() && FORMULA_PREFIXES.indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
