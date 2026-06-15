package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_hsn_b2b",
        indexes = {
                @Index(name = "idx_hsn_b2b_code", columnList = "hsn"),
                @Index(name = "idx_hsn_b2b_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1HsnB2b {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "hsn", length = 20, nullable = false)
    private String hsn;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "uqc", length = 20)
    private String uqc;

    @Column(name = "total_quantity", precision = 18, scale = 3)
    private BigDecimal totalQuantity;

    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "integrated_tax_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTaxAmount = BigDecimal.ZERO;

    @Column(name = "central_tax_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal centralTaxAmount = BigDecimal.ZERO;

    @Column(name = "state_ut_tax_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal stateUtTaxAmount = BigDecimal.ZERO;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}