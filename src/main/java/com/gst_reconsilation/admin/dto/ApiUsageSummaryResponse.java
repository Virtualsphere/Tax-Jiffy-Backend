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
    private int callCount;
    private int effectiveLimit;
    private Integer planDefaultLimit;
    private Integer override;
}
