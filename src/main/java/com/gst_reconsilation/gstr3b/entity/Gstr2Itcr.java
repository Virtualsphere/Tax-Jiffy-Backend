package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_itcr",
        indexes = {
                @Index(name = "idx_gstr2_itcr_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2Itcr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr2Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "description_for_reversal_of_itc", length = 255, nullable = false)
    private String descriptionForReversalOfItc;

    @Column(name = "to_be_added_or_reduced", length = 50)
    private String toBeAddedOrReduced;

    @Column(name = "itc_integrated_tax_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal itcIntegratedTaxAmount = BigDecimal.ZERO;

    @Column(name = "itc_central_tax_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal itcCentralTaxAmount = BigDecimal.ZERO;

    @Column(name = "itc_state_ut_tax_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal itcStateUtTaxAmount = BigDecimal.ZERO;

    @Column(name = "itc_cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal itcCessAmount = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}