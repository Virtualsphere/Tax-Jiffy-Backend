package com.gst_reconsilation.gstr3b.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Anchor record for a GSTR-3B period. GSTR-3B is a summary return that combines:
 *  - outward supply figures (already captured via {@code gstr1FilingId} -> Gstr1Filing)
 *  - inward/purchase register figures (already captured via {@code gstr2FilingId} -> Gstr2Filing)
 *  - ITC reconciliation against IMS + GSTR-2B (synced into Gstr3bImsInvoice / Gstr3bItcSummary)
 *
 * Challan generation and the actual filing submission (PUT /gstr3b/ret/save) are
 * intentionally NOT implemented here yet - this entity only supports the preview /
 * reconciliation flow up to table 6.1.
 */
@Entity
@Table(name = "gstr3b_filings",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_gstr3b_filings_Period",
                columnNames = {"CompanyGSTId", "FinancialYear", "TaxPeriod"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr3bFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    @Column(name = "FinancialYear", length = 10, nullable = false)
    private String financialYear;

    @Column(name = "TaxPeriod", length = 20, nullable = false)
    private String taxPeriod;

    /** FK to gstr1.entity.Gstr1Filing - kept as a plain id (no cross-module JPA join)
     *  to avoid coupling this module to the gstr1 package's entity mapping. */
    @Column(name = "gstr1_filing_id")
    private Integer gstr1FilingId;

    /** FK to Gstr2Filing (purchase register), same package. */
    @Column(name = "gstr2_filing_id")
    private Integer gstr2FilingId;

    @Column(name = "FilingStatus", length = 20, nullable = false)
    @Builder.Default
    private String filingStatus = "DRAFT";

    @Column(name = "two_b_sync_status", length = 20)
    private String twoBSyncStatus;

    @Column(name = "two_b_synced_at")
    private LocalDateTime twoBSyncedAt;

    @Column(name = "ims_filing_id")
    private Integer imsFilingId;

    // ── Manual entries (table 5.1 - Interest & Late Fee) ────────────
    // Real-time interest/late-fee calculation depends on payment ledgers this
    // module doesn't currently model, so these are captured as user-entered values
    // via PUT /api/gstr3b/filings/{id}/interest-late-fee.
    @Column(name = "interest_integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestIntegratedTax = BigDecimal.ZERO;

    @Column(name = "interest_central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestCentralTax = BigDecimal.ZERO;

    @Column(name = "interest_state_ut_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestStateUtTax = BigDecimal.ZERO;

    @Column(name = "interest_cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal interestCess = BigDecimal.ZERO;

    @Column(name = "late_fee_central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal lateFeeCentralTax = BigDecimal.ZERO;

    @Column(name = "late_fee_state_ut_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal lateFeeStateUtTax = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "updated_by")
    private Integer updatedBy;
}