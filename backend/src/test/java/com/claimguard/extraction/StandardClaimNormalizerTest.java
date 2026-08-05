package com.claimguard.extraction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StandardClaimNormalizerTest {

    private final StandardClaimNormalizer normalizer = new StandardClaimNormalizer();

    private static DocumentReader.ExtractedDocument document(String currency,
            String gender,
            List<String> procedures,
            BigDecimal totalAmount,
            List<DocumentReader.LineItem> lineItems) {
        return new DocumentReader.ExtractedDocument(
                "test-model",
                "  invoice ",
                "Asha Rao",
                "34",
                gender,
                "P-1",
                "City Hospital",
                "12 Main Street",
                "Fracture",
                procedures,
                "2026-01-01",
                "2026-01-03",
                "INV-1",
                "2026-01-03",
                totalAmount,
                currency,
                lineItems,
                Map.of(),
                "{}");
    }

    private static DocumentReader.LineItem item(String description,
            BigDecimal quantity,
            BigDecimal unitAmount,
            BigDecimal amount) {
        return new DocumentReader.LineItem(description, "CODE", quantity, unitAmount, amount);
    }

    @Test
    void normalisesRupeeVariantsToInr() {
        assertThat(normalizer.normalize(document("Rs.", null, List.of(), null, List.of())).currency())
                .isEqualTo("INR");
        assertThat(normalizer.normalize(document("rs", null, List.of(), null, List.of())).currency())
                .isEqualTo("INR");
        assertThat(normalizer.normalize(document("₹", null, List.of(), null, List.of())).currency())
                .isEqualTo("INR");
        assertThat(normalizer.normalize(document("usd", null, List.of(), null, List.of())).currency())
                .isEqualTo("USD");
    }

    @Test
    void normalisesGenderShorthand() {
        assertThat(normalizer.normalize(document(null, "m", List.of(), null, List.of())).patientGender())
                .isEqualTo("MALE");
        assertThat(normalizer.normalize(document(null, "Female", List.of(), null, List.of())).patientGender())
                .isEqualTo("FEMALE");
        assertThat(normalizer.normalize(document(null, "other", List.of(), null, List.of())).patientGender())
                .isEqualTo("other");
    }

    @Test
    void upperCasesTheDocumentTypeAndTrimsIt() {
        assertThat(normalizer.normalize(document(null, null, List.of(), null, List.of())).documentType())
                .isEqualTo("INVOICE");
    }

    @Test
    void dropsDuplicateAndBlankProcedures() {
        DocumentReader.ExtractedDocument result =
                normalizer.normalize(document(null, null, java.util.Arrays.asList("X-ray", "X-ray", "  ", null, "Cast"),
                        null, List.of()));

        assertThat(result.procedures()).containsExactly("X-ray", "Cast");
    }

    @Test
    void derivesLineItemAmountFromQuantityAndUnitPrice() {
        DocumentReader.ExtractedDocument result = normalizer.normalize(document(null, null, List.of(), null,
                List.of(item("Bed", new BigDecimal("3"), new BigDecimal("1200"), null))));

        assertThat(result.lineItems().get(0).amount()).isEqualByComparingTo("3600");
    }

    @Test
    void sumsLineItemsWhenNoTotalWasDeclared() {
        DocumentReader.ExtractedDocument result = normalizer.normalize(document(null, null, List.of(), null,
                List.of(item("Bed", null, null, new BigDecimal("1200")),
                        item("Scan", null, null, new BigDecimal("800")))));

        assertThat(result.totalAmount()).isEqualByComparingTo("2000");
    }

    @Test
    void keepsTheDeclaredTotalEvenWhenLineItemsDisagree() {
        DocumentReader.ExtractedDocument result = normalizer.normalize(document(null, null, List.of(),
                new BigDecimal("5000"),
                List.of(item("Bed", null, null, new BigDecimal("1200")))));

        assertThat(result.totalAmount()).isEqualByComparingTo("5000");
    }

    @Test
    void leavesTheTotalNullWhenThereIsNothingToSum() {
        assertThat(normalizer.normalize(document(null, null, List.of(), null, List.of())).totalAmount())
                .isNull();
    }
}
