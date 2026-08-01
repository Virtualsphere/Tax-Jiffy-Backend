// ewaybill/dto/EwaybillSyncByDateRequest.java
package com.gst_reconsilation.ewaybill.dto;

import lombok.Data;

@Data
public class EwaybillSyncByDateRequest {
    private Integer companyGstId;
    /** DD/MM/YYYY as the API expects, e.g. "02072024" per the sample — confirm exact format against sandbox before go-live */
    private String date;
}