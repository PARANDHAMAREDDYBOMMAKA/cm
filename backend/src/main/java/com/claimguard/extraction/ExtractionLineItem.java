package com.claimguard.extraction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "extraction_line_item")
@Getter
@Setter
public class ExtractionLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_id", nullable = false)
    private DocumentExtraction extraction;

    @Column(nullable = false)
    private int position;

    @Column(length = 512)
    private String description;

    @Column(length = 64)
    private String code;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_amount", precision = 14, scale = 2)
    private BigDecimal unitAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;
}
