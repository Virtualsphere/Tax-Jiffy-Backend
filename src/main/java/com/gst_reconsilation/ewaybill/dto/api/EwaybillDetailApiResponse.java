// ewaybill/dto/api/EwaybillDetailApiResponse.java
package com.gst_reconsilation.ewaybill.dto.api;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EwaybillDetailApiResponse {
    private Long ewbNo;
    private String ewayBillDate;
    private String genMode;
    private String userGstin;
    private String supplyType;
    private String subSupplyType;
    private String docType;
    private String docNo;
    private String docDate;
    private String fromGstin;
    private String fromTrdName;
    private String fromPlace;
    private Integer fromPincode;
    private Integer fromStateCode;
    private String toGstin;
    private String toTrdName;
    private String toPlace;
    private Integer toPincode;
    private Integer toStateCode;
    private BigDecimal totalValue;
    private BigDecimal totInvValue;
    private BigDecimal cgstValue;
    private BigDecimal sgstValue;
    private BigDecimal igstValue;
    private BigDecimal cessValue;
    private String transporterId;
    private String transporterName;
    private String status;
    private String validUpto;
    private Integer extendedTimes;
    private String rejectStatus;
    // itemList / VehiclListDetails intentionally omitted — see entity note above
}