// gstr3b/dto/Gstr3bFilingLinkRequest.java
package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;

@Data
public class Gstr3bFilingLinkRequest {
    private Integer companyGstId;
    private String financialYear;
    private String taxPeriod;
    private Integer gstr1FilingId;
    private Integer gstr2FilingId;
    private Integer imsFilingId;   // ← add this
}