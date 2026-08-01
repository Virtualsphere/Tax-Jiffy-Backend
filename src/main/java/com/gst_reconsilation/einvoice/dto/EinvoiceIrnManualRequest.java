// einvoice/dto/EinvoiceIrnManualRequest.java
package com.gst_reconsilation.einvoice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EinvoiceIrnManualRequest {
    private Integer companyGstId;
    private String retPeriod; // links to/creates the filing bucket this manual entry belongs to
    private String supplierGstin;
    private String irn;
    private Long ackNo;
    private String ackDt;
    private String docNum;
    private String docDate;
    private String docType;
    private String supplyType;
    private BigDecimal totInvAmt;
    private String irnStatus;
    private Long ewbNo;
    private String ewbDt;
    private String cnlDt;
    private String cnlRsn;
    private String cnlRem;
}