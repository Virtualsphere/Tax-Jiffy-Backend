package com.gst_reconsilation.subscription.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.entity.CompanyProfile;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per subscription purchase/upgrade event — append-only history. CompanyGST itself
 * stays a single mutable "current state" row (unchanged behavior); this table is purely
 * additive, written alongside it from CompanyGSTService.purchaseSubscription()/upgradeSubscription()
 * so the platform has an actual payment/invoice history to show, which didn't exist before.
 */
@Entity
@Table(name = "subscription_purchases",
        indexes = @Index(name = "idx_sub_purchase_gst", columnList = "CompanyGSTId"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    /** Denormalized so history reads correctly even if the GST number is ever edited. */
    @Column(name = "gst_number", length = 15, nullable = false)
    private String gstNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyId", nullable = false)
    private CompanyProfile company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubscriptionPlanId", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** Plan name/amount at the time of purchase — survives later edits to the plan itself. */
    @Column(name = "plan_name_snapshot", length = 150)
    private String planNameSnapshot;

    @Column(name = "plan_amount_snapshot", precision = 10, scale = 2)
    private BigDecimal planAmountSnapshot;

    /** PURCHASE | UPGRADE */
    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_payment_done", nullable = false)
    @Builder.Default
    private Boolean isPaymentDone = true;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by")
    private Integer createdBy;
}
