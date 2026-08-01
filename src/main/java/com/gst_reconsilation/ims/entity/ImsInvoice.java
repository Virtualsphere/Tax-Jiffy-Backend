// ims/entity/ImsInvoice.java
package com.gst_reconsilation.ims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One flattened row per supplier-reported invoice/note — same shape as the old
 * Gstr3bImsInvoice, now owned by its own module instead of gstr3b, and reachable
 * via three intake paths (see {@link #source}) instead of just the live API.
 */
@Entity
@Table(name = "ims_invoices",
        indexes = {
                @Index(name = "idx_ims_inv_filing", columnList = "filing_id"),
                @Index(name = "idx_ims_inv_supplier", columnList = "supplier_gstin"),
                @Index(name = "idx_ims_inv_number", columnList = "invoice_number")
        })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ImsInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private ImsFiling filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    /** GSTR1 or GSTR1A — null when the row came from MANUAL or EXCEL intake, since those don't distinguish. */
    @Column(name = "gstr_source", length = 10)
    private String gstrSource;

    /** B2B / B2BA / CDNR / CDNRA / ECOM_B2B / ECOM_URP2B / ECOMA_B2BA / ECOMA_URP2BA */
    @Column(name = "section", length = 20, nullable = false)
    private String section;

    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

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

    @Column(name = "place_of_supply", length = 50)
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

    /** Raw IMS action: ACCEPTED / REJECTED / PENDING */
    @Column(name = "ims_action", length = 20)
    private String imsAction;

    @Column(name = "remarks", length = 255)
    private String remarks;

    /** SYNC (live API) | MANUAL (single POST) | EXCEL (bulk template upload) */
    @Column(name = "source", length = 10, nullable = false)
    @Builder.Default
    private String source = "SYNC";

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}