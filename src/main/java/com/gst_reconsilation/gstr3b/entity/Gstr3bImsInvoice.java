package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One flattened row per supplier-reported invoice/note returned by the IMS
 * (Invoice Management System) API - GET /ims/supplierinvoices.
 * Covers both the "gstr1" (as-filed) and "gstr1a" (post-filing amendment) blocks,
 * and every section within them (b2b, b2ba, cdnr, cdnra, ecom.b2b, ecom.urp2b,
 * ecoma.b2ba, ecoma.urp2ba) - {@link #source} and {@link #section} record which.
 */
@Entity
@Table(name = "gstr3b_ims_invoice",
        indexes = {
                @Index(name = "idx_ims_filing", columnList = "filing_id"),
                @Index(name = "idx_ims_supplier", columnList = "supplier_gstin"),
                @Index(name = "idx_ims_invoice", columnList = "invoice_number")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr3bImsInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr3bFiling filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    /** "GSTR1" or "GSTR1A" */
    @Column(name = "source", length = 10, nullable = false)
    private String source;

    /** B2B / B2BA / CDNR / CDNRA / ECOM_B2B / ECOM_URP2B / ECOMA_B2BA / ECOMA_URP2BA */
    @Column(name = "section", length = 20, nullable = false)
    private String section;

    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

    /** Populated for ecom rows: rtin = registered recipient GSTIN, stin = e-com operator GSTIN */
    @Column(name = "recipient_gstin", length = 15)
    private String recipientGstin;

    @Column(name = "ecommerce_gstin", length = 15)
    private String ecommerceGstin;

    @Column(name = "original_invoice_number", length = 50)
    private String originalInvoiceNumber;

    @Column(name = "original_invoice_date")
    private LocalDate originalInvoiceDate;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_value", precision = 18, scale = 2)
    private BigDecimal invoiceValue;

    @Column(name = "place_of_supply", length = 10)
    private String placeOfSupply;

    @Column(name = "invoice_type", length = 20)
    private String invoiceType;

    @Column(name = "reverse_charge", length = 1)
    private String reverseCharge;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal taxableValue = BigDecimal.ZERO;

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

    /** Raw IMS action code: A = Accepted, R = Rejected, P = Pending (no action taken) */
    @Column(name = "ims_action", length = 1)
    private String imsAction;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}