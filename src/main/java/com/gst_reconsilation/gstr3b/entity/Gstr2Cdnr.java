package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_cdnr",
        indexes = {
                @Index(name = "idx_gstr2_cdnr_gstin", columnList = "gstin_of_supplier"),
                @Index(name = "idx_gstr2_cdnr_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2Cdnr {

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

    @Column(name = "note_refund_voucher_number", length = 50, nullable = false)
    private String noteRefundVoucherNumber;

    @Column(name = "note_refund_voucher_date")
    private LocalDate noteRefundVoucherDate;

    @Column(name = "invoice_advance_payment_voucher_number", length = 50)
    private String invoiceAdvancePaymentVoucherNumber;

    @Column(name = "invoice_advance_payment_voucher_date")
    private LocalDate invoiceAdvancePaymentVoucherDate;

    @Column(name = "pre_gst", length = 1)
    @Builder.Default
    private String preGst = "N";

    @Column(name = "document_type", length = 10)
    private String documentType;

    @Column(name = "reason_for_issuing_document", length = 100)
    private String reasonForIssuingDocument;

    @Column(name = "supply_type", length = 20)
    private String supplyType;

    @Column(name = "note_refund_voucher_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal noteRefundVoucherValue;

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
}