// ims/dto/ImsSyncRequest.java
package com.gst_reconsilation.ims.dto;

import lombok.Data;

@Data
public class ImsSyncRequest {
    private Integer companyGstId;
    /** MMYYYY, e.g. "102025" */
    private String retPeriod;
    private String section;  // Section Type expected by the IMS API
    private String rtnTyp;   // GSTR1 or GSTR1A
}