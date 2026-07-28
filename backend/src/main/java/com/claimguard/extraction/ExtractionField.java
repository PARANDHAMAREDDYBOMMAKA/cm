package com.claimguard.extraction;

import java.util.List;

public final class ExtractionField {

    public static final String DOCUMENT_TYPE = "documentType";
    public static final String PATIENT_NAME = "patientName";
    public static final String PATIENT_AGE = "patientAge";
    public static final String PATIENT_GENDER = "patientGender";
    public static final String PATIENT_ID = "patientId";
    public static final String PROVIDER_NAME = "providerName";
    public static final String PROVIDER_ADDRESS = "providerAddress";
    public static final String DIAGNOSIS = "diagnosis";
    public static final String PROCEDURES = "procedures";
    public static final String ADMISSION_DATE = "admissionDate";
    public static final String DISCHARGE_DATE = "dischargeDate";
    public static final String INVOICE_NUMBER = "invoiceNumber";
    public static final String INVOICE_DATE = "invoiceDate";
    public static final String TOTAL_AMOUNT = "totalAmount";
    public static final String CURRENCY = "currency";
    public static final String LINE_ITEMS = "lineItems";

    public static final List<String> ALL = List.of(
            DOCUMENT_TYPE,
            PATIENT_NAME,
            PATIENT_AGE,
            PATIENT_GENDER,
            PATIENT_ID,
            PROVIDER_NAME,
            PROVIDER_ADDRESS,
            DIAGNOSIS,
            PROCEDURES,
            ADMISSION_DATE,
            DISCHARGE_DATE,
            INVOICE_NUMBER,
            INVOICE_DATE,
            TOTAL_AMOUNT,
            CURRENCY,
            LINE_ITEMS);

    private ExtractionField() {
    }
}
