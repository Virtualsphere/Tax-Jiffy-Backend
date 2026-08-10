package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_ewaybill_reconciliation_result",
        indexes = @Index(name = "idx_gstr1_ewaybill_reco_filing", columnList = "filing_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Gstr1EwaybillReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    /** Null when this row represents a sale-register invoice with no e-way bill found (IN_SALE_REGISTER_ONLY). */
    @Column(name = "ewb_no")
    private Long ewbNo;

    @Column(name = "doc_no", length = 255)
    private String docNo;

    @Column(name = "recipient_gstin", length = 15)
    private String recipientGstin;

    /** Comma-joined GSTR-1 invoice numbers matched into this e-way bill's bucket (may be more than one). */
    @Column(name = "invoice_numbers", length = 500)
    private String invoiceNumbers;

    @Column(name = "ewaybill_value", precision = 18, scale = 2)
    private BigDecimal ewaybillValue;

    /** Sum of the matched invoices' invoiceValue — what's left in the bucket after subtracting this from ewaybillValue. */
    @Column(name = "sale_register_value", precision = 18, scale = 2)
    private BigDecimal saleRegisterValue;

    /** ewaybillValue - saleRegisterValue. Zero means the bucket is fully accounted for. */
    @Column(name = "difference", precision = 18, scale = 2)
    private BigDecimal difference;

    /** MATCHED | VALUE_MISMATCH | IN_SALE_REGISTER_ONLY | IN_EWAYBILL_ONLY */
    @Column(name = "match_status", length = 30, nullable = false)
    private String matchStatus;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}
