package com.gst_reconsilation.company.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class CompanyGSTResponse {
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
}
