// einvoice/dto/EinvoiceSyncRequest.java
package com.gst_reconsilation.einvoice.dto;

import lombok.Data;

@Data
public class EinvoiceSyncRequest {
    private Integer companyGstId;
    /** MMYYYY, e.g. "102025" */
    private String retPeriod;
}