package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One flattened row per GSTR-2B itcsumm leaf, from GET /gstr2b/all.
 * The government payload shape is: bucket -> category -> {igst,cgst,sgst,cess, ...subCategories}.
 * Rather than modelling ~20 nested classes 1:1, every leaf (both the category-level
 * total and each named sub-category under it) is stored as one row here, keyed by
 * {@link #bucket} / {@link #category} / {@link #subCategory} (null subCategory = the
 * category-level total row).
 *
 * bucket:    itcavl | itcunavl | itcrev | itcRejected
 * category:  nonrevsup | isdsup | revsup | imports | othersup
 * subCategory: b2b | b2ba | cdnr | cdnra | ecom | ecoma | isd | isda |
 *              impg | impgsez | impga | impgasez | cdnrrev | cdnrarev | null
 */
@Entity
@Table(name = "gstr3b_itc_summary",
        indexes = {
                @Index(name = "idx_itc_summary_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr3bItcSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr3bFiling filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "bucket", length = 20, nullable = false)
    private String bucket;

    @Column(name = "category", length = 20, nullable = false)
    private String category;

    @Column(name = "sub_category", length = 20)
    private String subCategory;

    @Column(name = "integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTax = BigDecimal.ZERO;

    @Column(name = "central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal centralTax = BigDecimal.ZERO;

    @Column(name = "state_ut_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal stateUtTax = BigDecimal.ZERO;

    @Column(name = "cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}