// ims/dto/ImsManualRequest.java
package com.gst_reconsilation.ims.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ImsManualRequest {
    private Integer companyGstId;
    private String retPeriod; // links to/creates the ImsFiling bucket this row belongs to
    private String section;
    private String supplierGstin;
    private String recipientGstin;
    private String ecommerceGstin;
    private String originalInvoiceNumber;
    private String originalInvoiceDate; // dd-MM-yyyy
    private String invoiceNumber;
    private String invoiceDate;         // dd-MM-yyyy
    private BigDecimal invoiceValue;
    private String placeOfSupply;
    private String invoiceType;
    private String reverseCharge;
    private BigDecimal rate;
    private BigDecimal taxableValue;
    private BigDecimal integratedTax;
    private BigDecimal centralTax;
    private BigDecimal stateUtTax;
    private BigDecimal cess;
    private String imsAction;
    private String remarks;
}