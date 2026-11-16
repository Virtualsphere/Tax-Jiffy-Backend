package com.gst_reconsilation.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ApiUsageSummaryResponse {
    private Integer companyGstId;
    private String gstNumber;
    private String companyName;
    private String apiType;
    private String periodKey;
    /** Calls of this apiType this period */
    private int callCount;
    /** Calls across ALL API types this period: this is what the limit applies to */
    private int totalCallCount;
    /** Monthly limit on totalCallCount */
    private int effectiveLimit;
    private Integer planDefaultLimit;
    private Integer override;
}
