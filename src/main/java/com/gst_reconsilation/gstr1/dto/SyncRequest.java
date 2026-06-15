// gstr1/sync/dto/SyncRequest.java
package com.gst_reconsilation.gstr1.dto;

import lombok.Data;

@Data
public class SyncRequest {
    private Integer companyGstId;
    private String financialYear;
    private String taxPeriod;
    private ApiCredentials credentials;
}