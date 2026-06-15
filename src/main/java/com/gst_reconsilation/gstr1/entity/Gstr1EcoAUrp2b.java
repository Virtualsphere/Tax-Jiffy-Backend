package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_ecoaurp2b",
        indexes = {
                @Index(name = "idx_ecoaurp2b_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1EcoAUrp2b {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "recipient_gstin", length = 15, nullable = false)
    private String recipientGstin;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "original_document_number", length = 50, nullable = false)
    private String originalDocumentNumber;

    @Column(name = "original_document_date", nullable = false)
    private LocalDate originalDocumentDate;

    @Column(name = "revised_document_number", length = 50, nullable = false)
    private String revisedDocumentNumber;

    @Column(name = "revised_document_date", nullable = false)
    private LocalDate revisedDocumentDate;

    @Column(name = "value_of_supplies_made", precision = 18, scale = 2, nullable = false)
    private BigDecimal valueOfSuppliesMade;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "place_of_supply", length = 50)
    private String placeOfSupply;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}