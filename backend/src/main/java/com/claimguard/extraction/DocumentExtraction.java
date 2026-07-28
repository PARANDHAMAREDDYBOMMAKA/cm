package com.claimguard.extraction;

import com.claimguard.claim.ClaimDocument;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_extraction")
@Getter
@Setter
public class DocumentExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private ClaimDocument document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExtractionStatus status = ExtractionStatus.PENDING;

    @Column(length = 128)
    private String model;

    @Column(name = "document_type", length = 64)
    private String documentType;

    @Column(name = "patient_name", length = 255)
    private String patientName;

    @Column(name = "patient_age", length = 32)
    private String patientAge;

    @Column(name = "patient_gender", length = 32)
    private String patientGender;

    @Column(name = "patient_id", length = 128)
    private String patientId;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Column(name = "provider_address", length = 512)
    private String providerAddress;

    @Column(length = 1024)
    private String diagnosis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> procedures = new ArrayList<>();

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "invoice_number", length = 128)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 8)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> confidence = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edited_fields", columnDefinition = "jsonb")
    private List<String> editedFields = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(length = 2000)
    private String error;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "extraction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<ExtractionLineItem> lineItems = new ArrayList<>();

    public void replaceLineItems(List<ExtractionLineItem> items) {
        lineItems.clear();
        for (int index = 0; index < items.size(); index++) {
            ExtractionLineItem item = items.get(index);
            item.setExtraction(this);
            item.setPosition(index);
            lineItems.add(item);
        }
    }

    public boolean wasEdited(String field) {
        return editedFields != null && editedFields.contains(field);
    }

    public void markEdited(String field) {
        if (editedFields == null) {
            editedFields = new ArrayList<>();
        }
        if (!editedFields.contains(field)) {
            editedFields.add(field);
        }
        confidence.put(field, 1.0);
    }
}
