package com.gst_reconsilation.company.dto;
import lombok.Data;

/**
 * No startDate/endDate here on purpose — the subscription period is always computed
 * server-side (now() -> now() + 1 month) in CompanyGSTService. A client-supplied date range
 * would let anyone grant themselves an arbitrarily long (or already-expired) subscription.
 */
@Data
public class PurchaseSubscriptionRequest {
    private Integer subscriptionPlanId;
}
