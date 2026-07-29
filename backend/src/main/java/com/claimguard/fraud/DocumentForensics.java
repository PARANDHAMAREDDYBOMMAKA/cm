package com.claimguard.fraud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_forensics")
@Getter
@Setter
public class DocumentForensics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(name = "has_metadata", nullable = false)
    private boolean hasMetadata;

    @Column(length = 255)
    private String software;

    @Column(name = "camera_make", length = 128)
    private String cameraMake;

    @Column(name = "camera_model", length = 128)
    private String cameraModel;

    @Column(name = "source_created_at")
    private Instant sourceCreatedAt;

    @Column(name = "source_modified_at")
    private Instant sourceModifiedAt;

    @Column(length = 255)
    private String producer;

    @Column(name = "ai_generated_score", precision = 5, scale = 4)
    private BigDecimal aiGeneratedScore;

    @Column(name = "ai_detector", length = 128)
    private String aiDetector;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
