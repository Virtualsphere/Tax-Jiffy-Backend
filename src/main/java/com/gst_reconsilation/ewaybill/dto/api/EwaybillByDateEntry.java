// ewaybill/dto/api/EwaybillByDateApiResponse.java
package com.gst_reconsilation.ewaybill.dto.api;

import lombok.Data;

@Data
public class EwaybillByDateEntry {
    private Long ewbNo;
    private String ewbDate;
    private String status;
    private String genGstin;
    private String docNo;
    private String docDate;
    private Integer delPinCode;
    private Integer delStateCode;
    private String delPlace;
    private String validUpto;
    private Integer extendedTimes;
    private String rejectStatus;
}