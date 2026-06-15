package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_cdnr",
        indexes = {
                @Index(name = "idx_cdnr_gstin", columnList = "gstin_of_recipient"),
                @Index(name = "idx_cdnr_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Cdnr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "gstin_of_recipient", length = 15, nullable = false)
    private String gstinOfRecipient;

    @Column(name = "receiver_name", length = 255)
    private String receiverName;

    @Column(name = "note_number", length = 50, nullable = false)
    private String noteNumber;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(name = "note_type", length = 1, nullable = false)
    private String noteType;

    @Column(name = "place_of_supply", length = 50)
    private String placeOfSupply;

    @Column(name = "reverse_charge", length = 1)
    @Builder.Default
    private String reverseCharge = "N";

    @Column(name = "note_supply_type", length = 50)
    private String noteSupplyType;

    @Column(name = "note_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal noteValue;

    @Column(name = "applicable_tax_rate_pct", precision = 5, scale = 2)
    private BigDecimal applicableTaxRatePct;

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