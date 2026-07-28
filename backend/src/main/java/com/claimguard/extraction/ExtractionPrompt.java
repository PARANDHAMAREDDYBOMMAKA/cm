package com.claimguard.extraction;

public final class ExtractionPrompt {

    public static final String TEXT = """
            You are reading a single medical insurance claim document from an Indian hospital.
            Return ONLY a JSON object, no prose and no markdown fence.

            Shape:
            {
              "documentType": one of DISCHARGE_SUMMARY | FINAL_BILL | PRESCRIPTION | LAB_REPORT | ID_PROOF | POLICY_PROOF | OTHER,
              "patientName": string|null,
              "patientAge": string|null,
              "patientGender": string|null,
              "patientId": string|null,
              "providerName": string|null,
              "providerAddress": string|null,
              "diagnosis": string|null,
              "procedures": [string],
              "admissionDate": "YYYY-MM-DD"|null,
              "dischargeDate": "YYYY-MM-DD"|null,
              "invoiceNumber": string|null,
              "invoiceDate": "YYYY-MM-DD"|null,
              "totalAmount": number|null,
              "currency": ISO code such as INR,
              "lineItems": [{"description": string, "code": string|null, "quantity": number|null, "unitAmount": number|null, "amount": number|null}],
              "confidence": { "<fieldName>": number between 0 and 1 }
            }

            Rules:
            - Use null when a field is not present on the document. Never invent a value.
            - Transcribe names, codes and numbers exactly as printed, including Indian-language text.
            - Amounts are plain numbers without currency symbols or thousands separators.
            - Indian bills group digits in the lakh style: 1,40,000.00 is 140000 and 12,34,567 is 1234567.
              Read every digit before the decimal point; never drop a leading group.
            - "totalAmount" is the final net payable after discounts, not the gross subtotal.
            - Give a confidence entry for every field you return, including "procedures" and "lineItems".
            - Confidence reflects how legible and unambiguous the source text was, not how plausible the value seems.
            """;

    private ExtractionPrompt() {
    }
}
