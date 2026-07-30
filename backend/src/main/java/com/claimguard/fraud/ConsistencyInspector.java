package com.claimguard.fraud;

import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimDocument;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.ExtractionLineItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class ConsistencyInspector {

    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("0.02");
    private static final BigDecimal CEILING_MULTIPLIER = new BigDecimal("1.5");

    public List<FraudSignal> inspect(Claim claim, ClaimDocument document, DocumentExtraction extraction) {
        List<FraudSignal> found = new ArrayList<>();
        found.addAll(totalSignals(claim, document, extraction));
        found.addAll(dateSignals(claim, document, extraction));
        found.addAll(clinicalSignals(claim, document, extraction));
        found.addAll(completenessSignals(claim, document, extraction));
        return found;
    }

    private List<FraudSignal> totalSignals(Claim claim, ClaimDocument document, DocumentExtraction extraction) {
        BigDecimal total = extraction.getTotalAmount();
        if (total == null || total.signum() <= 0 || extraction.getLineItems().isEmpty()) {
            return List.of();
        }
        BigDecimal summed = extraction.getLineItems().stream()
                .map(ExtractionLineItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (summed.signum() <= 0) {
            return List.of();
        }
        BigDecimal drift = summed.subtract(total).abs()
                .divide(total, 4, RoundingMode.HALF_UP);
        if (drift.compareTo(TOTAL_TOLERANCE) <= 0) {
            return List.of();
        }
        return List.of(SignalBuilder.of(claim.getId(), document.getId(), SignalType.LINE_ITEM_MISMATCH,
                Severity.MEDIUM,
                "The line items add up to " + summed.toPlainString() + " but the bill total says "
                        + total.toPlainString() + ".",
                Map.of("lineItemTotal", summed.toPlainString(),
                        "billTotal", total.toPlainString(),
                        "drift", drift.movePointRight(2).setScale(1, RoundingMode.HALF_UP) + "%")));
    }

    private List<FraudSignal> dateSignals(Claim claim, ClaimDocument document, DocumentExtraction extraction) {
        List<FraudSignal> found = new ArrayList<>();
        LocalDate admission = extraction.getAdmissionDate();
        LocalDate discharge = extraction.getDischargeDate();
        LocalDate invoiced = extraction.getInvoiceDate();
        LocalDate today = LocalDate.now();

        if (admission != null && discharge != null && discharge.isBefore(admission)) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.DATE_INCONSISTENCY,
                    Severity.MEDIUM,
                    "The discharge date is before the admission date.",
                    Map.of("admission", admission.toString(), "discharge", discharge.toString())));
        }

        if (admission != null && invoiced != null && invoiced.isBefore(admission)) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.DATE_INCONSISTENCY,
                    Severity.MEDIUM,
                    "The invoice is dated before the patient was admitted.",
                    Map.of("admission", admission.toString(), "invoice", invoiced.toString())));
        }

        for (Map.Entry<String, LocalDate> entry : dated(admission, discharge, invoiced).entrySet()) {
            if (entry.getValue().isAfter(today)) {
                found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.DATE_INCONSISTENCY,
                        Severity.MEDIUM,
                        "The " + entry.getKey() + " date is in the future.",
                        Map.of(entry.getKey(), entry.getValue().toString())));
            }
        }

        return found;
    }

    private List<FraudSignal> clinicalSignals(Claim claim, ClaimDocument document, DocumentExtraction extraction) {
        Optional<ClinicalReference.Condition> matched = ClinicalReference.match(extraction.getDiagnosis());
        if (matched.isEmpty()) {
            return List.of();
        }
        ClinicalReference.Condition condition = matched.get();
        List<FraudSignal> found = new ArrayList<>();

        List<String> treatments = new ArrayList<>(extraction.getProcedures());
        extraction.getLineItems().forEach(item -> treatments.add(item.getDescription()));
        if (!treatments.isEmpty() && !ClinicalReference.isTreated(condition, treatments)) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.PROCEDURE_DIAGNOSIS_MISMATCH,
                    Severity.MEDIUM,
                    "Nothing billed here is a usual treatment for " + condition.label() + ".",
                    Map.of("diagnosis", String.valueOf(extraction.getDiagnosis()),
                            "condition", condition.label())));
        }

        BigDecimal total = extraction.getTotalAmount();
        BigDecimal ceiling = condition.ceiling().multiply(CEILING_MULTIPLIER);
        if (total != null && total.compareTo(ceiling) > 0) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.AMOUNT_OUT_OF_BAND,
                    Severity.HIGH,
                    "The total is far above what " + condition.label() + " normally costs.",
                    Map.of("billTotal", total.toPlainString(),
                            "typicalCeiling", condition.ceiling().toPlainString(),
                            "condition", condition.label())));
        }

        return found;
    }

    private List<FraudSignal> completenessSignals(Claim claim, ClaimDocument document, DocumentExtraction extraction) {
        List<String> missing = new ArrayList<>();
        if (extraction.getTotalAmount() == null) {
            missing.add("total amount");
        }
        if (isBlank(extraction.getProviderName())) {
            missing.add("provider name");
        }
        if (isBlank(extraction.getPatientName())) {
            missing.add("patient name");
        }
        if (missing.isEmpty()) {
            return List.of();
        }
        return List.of(SignalBuilder.of(claim.getId(), document.getId(), SignalType.MISSING_CRITICAL_FIELD,
                Severity.LOW,
                "The document is missing " + String.join(", ", missing) + ".",
                Map.of("missing", String.join(", ", missing))));
    }

    private static Map<String, LocalDate> dated(LocalDate admission, LocalDate discharge, LocalDate invoiced) {
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        if (admission != null) {
            dates.put("admission", admission);
        }
        if (discharge != null) {
            dates.put("discharge", discharge);
        }
        if (invoiced != null) {
            dates.put("invoice", invoiced);
        }
        return dates;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
