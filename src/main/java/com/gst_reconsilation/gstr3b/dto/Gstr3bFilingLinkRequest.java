package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;

@Data
public class Gstr3bFilingLinkRequest {
    private Integer companyGstId;
    private String financialYear;
    private String taxPeriod;
    /** Optional - id of the matching Gstr1Filing (outward supplies for this period) */
    private Integer gstr1FilingId;
    /** Optional - id of the matching Gstr2Filing (purchase register for this period) */
    private Integer gstr2FilingId;
}