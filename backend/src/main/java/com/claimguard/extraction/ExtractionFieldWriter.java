package com.claimguard.extraction;

import com.claimguard.support.Values;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class ExtractionFieldWriter {

    private static final Map<String, BiConsumer<DocumentExtraction, String>> WRITERS = Map.ofEntries(
            Map.entry(ExtractionField.DOCUMENT_TYPE,
                    (target, value) -> target.setDocumentType(Values.truncate(value, 64))),
            Map.entry(ExtractionField.PATIENT_NAME,
                    (target, value) -> target.setPatientName(Values.truncate(value, 255))),
            Map.entry(ExtractionField.PATIENT_AGE,
                    (target, value) -> target.setPatientAge(Values.truncate(value, 32))),
            Map.entry(ExtractionField.PATIENT_GENDER,
                    (target, value) -> target.setPatientGender(Values.truncate(value, 32))),
            Map.entry(ExtractionField.PATIENT_ID,
                    (target, value) -> target.setPatientId(Values.truncate(value, 128))),
            Map.entry(ExtractionField.PROVIDER_NAME,
                    (target, value) -> target.setProviderName(Values.truncate(value, 255))),
            Map.entry(ExtractionField.PROVIDER_ADDRESS,
                    (target, value) -> target.setProviderAddress(Values.truncate(value, 512))),
            Map.entry(ExtractionField.DIAGNOSIS,
                    (target, value) -> target.setDiagnosis(Values.truncate(value, 1024))),
            Map.entry(ExtractionField.PROCEDURES,
                    (target, value) -> target.setProcedures(splitList(value))),
            Map.entry(ExtractionField.ADMISSION_DATE,
                    (target, value) -> target.setAdmissionDate(requireDate(ExtractionField.ADMISSION_DATE, value))),
            Map.entry(ExtractionField.DISCHARGE_DATE,
                    (target, value) -> target.setDischargeDate(requireDate(ExtractionField.DISCHARGE_DATE, value))),
            Map.entry(ExtractionField.INVOICE_NUMBER,
                    (target, value) -> target.setInvoiceNumber(Values.truncate(value, 128))),
            Map.entry(ExtractionField.INVOICE_DATE,
                    (target, value) -> target.setInvoiceDate(requireDate(ExtractionField.INVOICE_DATE, value))),
            Map.entry(ExtractionField.TOTAL_AMOUNT,
                    (target, value) -> target.setTotalAmount(requireDecimal(ExtractionField.TOTAL_AMOUNT, value))),
            Map.entry(ExtractionField.CURRENCY,
                    (target, value) -> target.setCurrency(Values.truncate(value, 8))));

    private ExtractionFieldWriter() {
    }

    public static void write(DocumentExtraction extraction, String field, String value) {
        BiConsumer<DocumentExtraction, String> writer = WRITERS.get(field);
        if (writer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field is not editable: " + field);
        }
        writer.accept(extraction, value);
        extraction.markEdited(field);
    }

    private static List<String> splitList(String value) {
        String trimmed = Values.text(value);
        if (trimmed == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(trimmed.split("[\\n,]"))
                .map(Values::text)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LocalDate requireDate(String field, String value) {
        if (Values.text(value) == null) {
            return null;
        }
        LocalDate parsed = Values.date(value);
        if (parsed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid date for " + field + ": " + value);
        }
        return parsed;
    }

    private static BigDecimal requireDecimal(String field, String value) {
        if (Values.text(value) == null) {
            return null;
        }
        BigDecimal parsed = Values.decimal(value);
        if (parsed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid amount for " + field + ": " + value);
        }
        return parsed;
    }
}
