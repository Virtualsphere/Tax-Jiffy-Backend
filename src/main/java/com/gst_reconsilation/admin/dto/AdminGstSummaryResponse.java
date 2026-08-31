package com.gst_reconsilation.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class AdminGstSummaryResponse {
    private Integer id;
    private String gstNumber;
    private Integer companyId;
    private String companyName;
    private Integer subscriptionPlanId;
    private String subscriptionPlanName;
    private BigDecimal planAmount;
    private Integer planUserCount;
    private Integer planTransactionCount;
    private Boolean isPaymentDone;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private long activeUserCount;
    private long pageAccessCount;
}
