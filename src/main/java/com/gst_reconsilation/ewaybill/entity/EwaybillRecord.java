// ewaybill/entity/EwaybillRecord.java
package com.gst_reconsilation.ewaybill.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ewaybill_records",
        uniqueConstraints = @UniqueConstraint(name = "UQ_ewaybill_filing_ewbno", columnNames = {"filing_id", "ewb_no"}),
        indexes = @Index(name = "idx_ewb_filing", columnList = "filing_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EwaybillRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private EwaybillFiling filing;

    @Column(name = "ewb_no", nullable = false)
    private Long ewbNo;

    @Column(name = "ewb_date", length = 30)
    private String ewbDate;

    @Column(name = "gen_mode", length = 10)
    private String genMode;

    @Column(name = "user_gstin", length = 15)
    private String userGstin;

    @Column(name = "supply_type", length = 5)
    private String supplyType;

    @Column(name = "sub_supply_type", length = 5)
    private String subSupplyType;

    @Column(name = "doc_type", length = 10)
    private String docType;

    @Column(name = "doc_no", length = 50)
    private String docNo;

    @Column(name = "doc_date", length = 20)
    private String docDate;

    @Column(name = "from_gstin", length = 15)
    private String fromGstin;

    @Column(name = "from_trd_name", length = 255)
    private String fromTrdName;

    @Column(name = "from_place", length = 100)
    private String fromPlace;

    @Column(name = "from_pincode")
    private Integer fromPincode;

    @Column(name = "from_state_code")
    private Integer fromStateCode;

    @Column(name = "to_gstin", length = 15)
    private String toGstin;

    @Column(name = "to_trd_name", length = 255)
    private String toTrdName;

    @Column(name = "to_place", length = 100)
    private String toPlace;

    @Column(name = "to_pincode")
    private Integer toPincode;

    @Column(name = "to_state_code")
    private Integer toStateCode;

    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "tot_inv_value", precision = 18, scale = 2)
    private BigDecimal totInvValue;

    @Column(name = "cgst_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cgstValue = BigDecimal.ZERO;

    @Column(name = "sgst_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal sgstValue = BigDecimal.ZERO;

    @Column(name = "igst_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal igstValue = BigDecimal.ZERO;

    @Column(name = "cess_value", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessValue = BigDecimal.ZERO;

    @Column(name = "transporter_id", length = 15)
    private String transporterId;

    @Column(name = "transporter_name", length = 255)
    private String transporterName;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "valid_upto", length = 30)
    private String validUpto;

    @Column(name = "extended_times")
    private Integer extendedTimes;

    @Column(name = "reject_status", length = 5)
    private String rejectStatus;

    @Column(name = "source", length = 10, nullable = false)
    @Builder.Default
    private String source = "SYNC"; // SYNC | MANUAL

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;
}