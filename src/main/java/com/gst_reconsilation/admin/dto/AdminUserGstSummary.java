package com.gst_reconsilation.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AdminUserGstSummary {
    private Integer companyGstId;
    private String gstNumber;
    private Boolean isPaymentDone;
    private String subscriptionPlanName;
    private Boolean isActive;
}
