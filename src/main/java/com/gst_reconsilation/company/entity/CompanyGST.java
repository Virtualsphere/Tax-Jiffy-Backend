package com.gst_reconsilation.company.entity;

import com.gst_reconsilation.subscription.entity.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CompanyGST",
        uniqueConstraints = @UniqueConstraint(name = "UQ_CompanyGST_GSTNumber", columnNames = "GSTNumber"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyGST {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyId", nullable = false)
    private CompanyProfile company;

    @Column(name = "GSTNumber", length = 15, nullable = false)
    private String gstNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubscriptionPlanId")
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "IsPaymentDone")
    private Boolean isPaymentDone;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "updated_by")
    private Integer updatedBy;
}
