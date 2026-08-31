package com.gst_reconsilation.subscription.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class SubscriptionPurchaseResponse {
    private Integer id;
    private String gstNumber;
    private Integer companyId;
    private String companyName;
    private Integer subscriptionPlanId;
    private String planNameSnapshot;
    private BigDecimal planAmountSnapshot;
    private String transactionType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isPaymentDone;
    private LocalDate createdDate;
    private Integer createdBy;
}
