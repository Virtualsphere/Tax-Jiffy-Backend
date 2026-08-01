// einvoice/dto/EinvoiceSyncResponse.java
package com.gst_reconsilation.einvoice.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class EinvoiceSyncResponse {
    private Integer filingId;
    private String retPeriod;
    private String syncStatus;
    private int hsnRowsSynced;
    private int irnRowsSynced;
}