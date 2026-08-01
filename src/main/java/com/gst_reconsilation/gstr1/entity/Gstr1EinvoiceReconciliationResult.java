
package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_einvoice_reconciliation_result",
        indexes = @Index(name = "idx_gstr1_einvoice_reco_filing", columnList = "filing_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Gstr1EinvoiceReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "recipient_gstin", length = 15)
    private String recipientGstin;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    /** MATCHED | VALUE_MISMATCH | IN_SALE_REGISTER_ONLY | IN_EINVOICE_ONLY */
    @Column(name = "match_status", length = 30, nullable = false)
    private String matchStatus;

    @Column(name = "sale_register_invoice_value", precision = 18, scale = 2)
    private BigDecimal saleRegisterInvoiceValue;

    @Column(name = "sale_register_taxable_value", precision = 18, scale = 2)
    private BigDecimal saleRegisterTaxableValue;

    @Column(name = "einvoice_invoice_value", precision = 18, scale = 2)
    private BigDecimal einvoiceInvoiceValue;

    @Column(name = "einvoice_irn", length = 100)
    private String einvoiceIrn;

    @Column(name = "einvoice_status", length = 10)
    private String einvoiceStatus;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}