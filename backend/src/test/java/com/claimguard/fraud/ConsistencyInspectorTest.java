package com.claimguard.fraud;

import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimDocument;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.ExtractionLineItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistencyInspectorTest {

    private final ConsistencyInspector inspector = new ConsistencyInspector();

    private static Claim claim() {
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setReference("CLM-TEST");
        claim.setClaimantName("Ramesh Kumar Sharma");
        return claim;
    }

    private static ClaimDocument document(Claim claim) {
        ClaimDocument document = new ClaimDocument();
        document.setId(UUID.randomUUID());
        document.setClaim(claim);
        document.setOriginalFilename("bill.pdf");
        return document;
    }

    private static DocumentExtraction cataractBill() {
        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setPatientName("Ramesh Kumar Sharma");
        extraction.setProviderName("Lakeview Eye & General Hospital");
        extraction.setDiagnosis("Cataract (Right Eye) - Immature Senile Cataract");
        extraction.setAdmissionDate(LocalDate.of(2026, 7, 10));
        extraction.setDischargeDate(LocalDate.of(2026, 7, 10));
        extraction.setInvoiceDate(LocalDate.of(2026, 7, 10));
        extraction.setTotalAmount(new BigDecimal("64200"));
        extraction.setLineItems(lineItems(
                item("Phacoemulsification with IOL Implantation", "55000"),
                item("Operation Theatre Charges", "6000"),
                item("Pharmacy & Consumables", "3200")));
        return extraction;
    }

    private static ExtractionLineItem item(String description, String amount) {
        ExtractionLineItem line = new ExtractionLineItem();
        line.setDescription(description);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static List<ExtractionLineItem> lineItems(ExtractionLineItem... items) {
        return new ArrayList<>(List.of(items));
    }

    private List<SignalType> inspect(DocumentExtraction extraction) {
        Claim claim = claim();
        return inspector.inspect(claim, document(claim), extraction).stream()
                .map(FraudSignal::getType)
                .toList();
    }

    @Test
    void aCoherentBillRaisesNothing() {
        assertThat(inspect(cataractBill())).isEmpty();
    }

    @Test
    void lineItemsThatDoNotSumToTheTotalAreFlagged() {
        DocumentExtraction extraction = cataractBill();
        extraction.setTotalAmount(new BigDecimal("450000"));

        assertThat(inspect(extraction)).contains(SignalType.LINE_ITEM_MISMATCH);
    }

    @Test
    void smallRoundingDriftIsTolerated() {
        DocumentExtraction extraction = cataractBill();
        extraction.setTotalAmount(new BigDecimal("64500"));

        assertThat(inspect(extraction)).doesNotContain(SignalType.LINE_ITEM_MISMATCH);
    }

    @Test
    void dischargeBeforeAdmissionIsFlagged() {
        DocumentExtraction extraction = cataractBill();
        extraction.setDischargeDate(LocalDate.of(2026, 7, 4));

        assertThat(inspect(extraction)).contains(SignalType.DATE_INCONSISTENCY);
    }

    @Test
    void aFutureDateIsFlagged() {
        DocumentExtraction extraction = cataractBill();
        extraction.setDischargeDate(LocalDate.now().plusDays(30));

        assertThat(inspect(extraction)).contains(SignalType.DATE_INCONSISTENCY);
    }

    @Test
    void treatmentUnrelatedToTheDiagnosisIsFlagged() {
        DocumentExtraction extraction = cataractBill();
        extraction.setLineItems(lineItems(
                item("Titanium Bone Plate", "40000"),
                item("Orthopaedic Implant Set", "24200")));

        assertThat(inspect(extraction)).contains(SignalType.PROCEDURE_DIAGNOSIS_MISMATCH);
    }

    @Test
    void anAmountFarAboveTheTariffBandIsFlagged() {
        DocumentExtraction extraction = cataractBill();
        extraction.setTotalAmount(new BigDecimal("450000"));
        extraction.setLineItems(lineItems(item("Phacoemulsification with IOL Implantation", "450000")));

        assertThat(inspect(extraction)).contains(SignalType.AMOUNT_OUT_OF_BAND);
    }

    @Test
    void missingCriticalFieldsAreFlagged() {
        DocumentExtraction extraction = cataractBill();
        extraction.setProviderName(null);
        extraction.setTotalAmount(null);

        assertThat(inspect(extraction)).contains(SignalType.MISSING_CRITICAL_FIELD);
    }
}
