package com.gst_reconsilation.subscription.dto;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class SubscriptionPlanRequest {
    private String name;
    private Integer userCount;
    private Integer transactionCount;
    private BigDecimal planAmount;
}
