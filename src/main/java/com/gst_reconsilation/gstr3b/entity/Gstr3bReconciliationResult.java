// gstr3b/entity/Gstr3bReconciliationResult.java
package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr3b_reconciliation_result",
        indexes = @Index(name = "idx_recon_filing", columnList = "filing_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Gstr3bReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr3bFiling filing;

    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    /** MATCHED | VALUE_MISMATCH | IN_PURCHASE_REGISTER_ONLY | IN_IMS_ONLY */
    @Column(name = "match_status", length = 30, nullable = false)
    private String matchStatus;

    @Column(name = "purchase_register_taxable_value", precision = 18, scale = 2)
    private BigDecimal purchaseRegisterTaxableValue;

    @Column(name = "ims_taxable_value", precision = 18, scale = 2)
    private BigDecimal imsTaxableValue;

    @Column(name = "purchase_register_igst", precision = 18, scale = 2)
    private BigDecimal purchaseRegisterIgst;

    @Column(name = "ims_igst", precision = 18, scale = 2)
    private BigDecimal imsIgst;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}