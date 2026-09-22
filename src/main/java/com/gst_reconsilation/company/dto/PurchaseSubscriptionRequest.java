package com.gst_reconsilation.company.dto;
import lombok.Data;

/**
 * Only the plan is caller-supplied. The subscription period is computed
 * server-side (always one month) - taking it from the request would let a
 * caller grant itself an arbitrarily long subscription.
 */
@Data
public class PurchaseSubscriptionRequest {
    private Integer subscriptionPlanId;
}
