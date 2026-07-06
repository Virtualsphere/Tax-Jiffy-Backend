package com.gst_reconsilation.company.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class PurchaseSubscriptionRequest {
    private Integer subscriptionPlanId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
