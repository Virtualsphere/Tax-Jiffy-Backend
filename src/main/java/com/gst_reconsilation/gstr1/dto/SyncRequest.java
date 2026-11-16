// gstr1/sync/dto/SyncRequest.java
package com.gst_reconsilation.gstr1.dto;

import lombok.Data;

/**
 * GST API credentials are no longer sent by the client: the server uses its own Whitebooks
 * keys plus the per-GSTIN session created via /api/gst-session/{companyGstId}.
 */
@Data
public class SyncRequest {
    private Integer companyGstId;
    private String financialYear;
    private String taxPeriod;
}
