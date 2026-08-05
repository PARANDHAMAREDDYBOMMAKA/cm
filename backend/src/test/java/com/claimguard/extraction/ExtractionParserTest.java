package com.claimguard.extraction;

import com.claimguard.ai.AiRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtractionParserTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private DocumentReader.ExtractedDocument parse(String text) {
        return ExtractionParser.parse(mapper, "test-model", text);
    }

    @Test
    void readsAPlainJsonObject() {
        DocumentReader.ExtractedDocument result = parse("""
                {"documentType":"INVOICE","patientName":"Asha Rao","totalAmount":1234.50,"currency":"INR"}
                """);

        assertThat(result.model()).isEqualTo("test-model");
        assertThat(result.documentType()).isEqualTo("INVOICE");
        assertThat(result.patientName()).isEqualTo("Asha Rao");
        assertThat(result.totalAmount()).isEqualByComparingTo("1234.50");
        assertThat(result.currency()).isEqualTo("INR");
    }

    @Test
    void stripsAMarkdownCodeFence() {
        DocumentReader.ExtractedDocument result = parse("""
                ```json
                {"invoiceNumber":"INV-7"}
                ```
                """);

        assertThat(result.invoiceNumber()).isEqualTo("INV-7");
    }

    @Test
    void stripsReasoningEmittedBeforeTheAnswer() {
        DocumentReader.ExtractedDocument result =
                parse("<think>The bill totals 900.</think>{\"invoiceNumber\":\"INV-9\"}");

        assertThat(result.invoiceNumber()).isEqualTo("INV-9");
    }

    @Test
    void ignoresProseAroundTheJsonObject() {
        DocumentReader.ExtractedDocument result =
                parse("Here is the extraction:\n{\"patientId\":\"P-3\"}\nHope that helps!");

        assertThat(result.patientId()).isEqualTo("P-3");
    }

    @Test
    void parsesAmountsGivenAsStrings() {
        assertThat(parse("{\"totalAmount\":\"1,234.50\"}").totalAmount()).isEqualByComparingTo("1234.50");
    }

    @Test
    void treatsMissingAndNullFieldsAsAbsent() {
        DocumentReader.ExtractedDocument result = parse("{\"patientName\":null}");

        assertThat(result.patientName()).isNull();
        assertThat(result.diagnosis()).isNull();
        assertThat(result.totalAmount()).isNull();
        assertThat(result.procedures()).isEmpty();
        assertThat(result.lineItems()).isEmpty();
    }

    @Test
    void readsLineItemsAndSkipsEmptyRows() {
        DocumentReader.ExtractedDocument result = parse("""
                {"lineItems":[
                  {"description":"Bed charges","quantity":2,"unitAmount":1000,"amount":2000},
                  {"code":"ONLY-CODE"},
                  {"description":"Scan","amount":"800"}
                ]}
                """);

        assertThat(result.lineItems()).hasSize(2);
        assertThat(result.lineItems().get(0).description()).isEqualTo("Bed charges");
        assertThat(result.lineItems().get(0).amount()).isEqualByComparingTo("2000");
        assertThat(result.lineItems().get(1).description()).isEqualTo("Scan");
    }

    @Test
    void readsConfidenceScoresForKnownFieldsOnly() {
        DocumentReader.ExtractedDocument result =
                parse("{\"confidence\":{\"patientName\":0.91,\"nonsense\":0.5}}");

        assertThat(result.confidence()).containsEntry("patientName", 0.91);
        assertThat(result.confidence()).doesNotContainKey("nonsense");
    }

    @Test
    void rejectsAResponseThatIsNotJson() {
        assertThatThrownBy(() -> parse("I could not read this document."))
                .isInstanceOf(AiRequestException.class);
    }
}
