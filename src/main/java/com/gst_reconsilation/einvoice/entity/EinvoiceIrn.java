// einvoice/entity/EinvoiceIrn.java
package com.gst_reconsilation.einvoice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "einvoice_irn",
        uniqueConstraints = @UniqueConstraint(name = "UQ_einvoice_irn", columnNames = "irn"),
        indexes = @Index(name = "idx_irn_filing", columnList = "filing_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EinvoiceIrn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private EinvoiceFiling filing;

    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

    @Column(name = "irn", length = 100, nullable = false)
    private String irn;

    @Column(name = "ack_no")
    private Long ackNo;

    @Column(name = "ack_dt", length = 30)
    private String ackDt;

    @Column(name = "doc_num", length = 50)
    private String docNum;

    @Column(name = "doc_date", length = 20)
    private String docDate;

    @Column(name = "doc_type", length = 10)
    private String docType;

    @Column(name = "supply_type", length = 10)
    private String supplyType;

    @Column(name = "tot_inv_amt", precision = 18, scale = 2)
    private BigDecimal totInvAmt;

    @Column(name = "irn_status", length = 10)
    private String irnStatus;

    @Column(name = "ewb_no")
    private Long ewbNo;

    @Column(name = "ewb_dt", length = 30)
    private String ewbDt;

    @Column(name = "ewb_valid_till", length = 30)
    private String ewbValidTill;

    @Column(name = "cnl_dt", length = 30)
    private String cnlDt;

    @Column(name = "cnl_rsn", length = 255)
    private String cnlRsn;

    @Column(name = "cnl_rem", length = 255)
    private String cnlRem;

    @Column(name = "remarks", length = 255)
    private String remarks;

    /** JWT-encoded full invoice payload — populated only when irndtl is fetched, not on the basic irnlist sync. */
    @Column(name = "signed_invoice", columnDefinition = "TEXT")
    private String signedInvoice;

    @Column(name = "signed_qr_code", columnDefinition = "TEXT")
    private String signedQrCode;

    @Column(name = "source", length = 10, nullable = false)
    @Builder.Default
    private String source = "SYNC"; // SYNC | MANUAL

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "is_paired", nullable = false)
    @Builder.Default
    private Boolean isPaired = false;

    @Column(name = "paired_gstr1_b2b_id")
    private Integer pairedGstr1B2bId;
}