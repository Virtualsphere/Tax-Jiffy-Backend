// ewaybill/dto/EwaybillManualRequest.java
package com.gst_reconsilation.ewaybill.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EwaybillManualRequest {
    private Integer companyGstId;
    private Long ewbNo;
    private String ewbDate;
    private String docType;
    private String docNo;
    private String docDate;
    private String fromGstin;
    private String fromTrdName;
    private String fromPlace;
    private String toGstin;
    private String toTrdName;
    private String toPlace;
    private BigDecimal totalValue;
    private BigDecimal totInvValue;
    private BigDecimal cgstValue;
    private BigDecimal sgstValue;
    private BigDecimal igstValue;
    private BigDecimal cessValue;
    private String transporterName;
    private String status;
    private String validUpto;
}