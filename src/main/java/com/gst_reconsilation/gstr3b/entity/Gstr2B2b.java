package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_b2b",
        indexes = {
                @Index(name = "idx_gstr2_b2b_gstin", columnList = "gstin_of_supplier"),
                @Index(name = "idx_gstr2_b2b_invoice", columnList = "invoice_number"),
                @Index(name = "idx_gstr2_b2b_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2B2b {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr2Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "gstin_of_supplier", length = 15, nullable = false)
    private String gstinOfSupplier;

    @Column(name = "invoice_number", length = 50, nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_value", precision = 18, scale = 2)
    private BigDecimal invoiceValue;

    @Column(name = "place_of_supply", length = 50)
    private String placeOfSupply;

    @Column(name = "reverse_charge", length = 1)
    @Builder.Default
    private String reverseCharge = "N";

    @Column(name = "invoice_type", length = 50)
    private String invoiceType;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "integrated_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTaxPaid = BigDecimal.ZERO;

    @Column(name = "central_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal centralTaxPaid = BigDecimal.ZERO;

    @Column(name = "state_ut_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal stateUtTaxPaid = BigDecimal.ZERO;

    @Column(name = "cess_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessPaid = BigDecimal.ZERO;

    @Column(name = "eligibility_for_itc", length = 50)
    private String eligibilityForItc;

    @Column(name = "availed_itc_integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcIntegratedTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcCentralTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_state_ut_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcStateUtTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcCess = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    // ── GSTR-2B ⇄ IMS reconciliation correction tracking ──
    // First-touch snapshot of the as-uploaded value, populated only once, the first time
    // this row is corrected — mirrors the isPaired/pairedIrn convention already used on
    // Gstr1B2b for its own reconciliation flow.
    @Column(name = "original_taxable_value", precision = 18, scale = 2)
    private BigDecimal originalTaxableValue;
    @Column(name = "original_integrated_tax_paid", precision = 18, scale = 2)
    private BigDecimal originalIntegratedTaxPaid;
    @Column(name = "original_central_tax_paid", precision = 18, scale = 2)
    private BigDecimal originalCentralTaxPaid;
    @Column(name = "original_state_ut_tax_paid", precision = 18, scale = 2)
    private BigDecimal originalStateUtTaxPaid;
    @Column(name = "original_cess_paid", precision = 18, scale = 2)
    private BigDecimal originalCessPaid;

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private boolean edited = false;

    /** ACCEPT / REJECT / PENDING — the user's GSTR-2B ⇄ IMS reconciliation decision for this row. */
    @Column(name = "reconciliation_action", length = 10, nullable = false)
    @Builder.Default
    private String reconciliationAction = "PENDING";

    @Column(name = "reconciliation_remarks", length = 255)
    private String reconciliationRemarks;
}