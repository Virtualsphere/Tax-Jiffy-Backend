package com.gst_reconsilation.apiusage.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * How many outbound 3rd-party GST-portal API calls (e-invoice/e-way bill/IMS) a GST number has
 * made in a given period ("yyyy-MM", monthly reset). One row per (companyGST, apiType, periodKey),
 * incremented and limit-checked atomically by ApiUsageService.recordAndEnforce() before every
 * outbound call — nothing tracked usage before this.
 */
@Entity
@Table(name = "api_usage_counters",
        uniqueConstraints = @UniqueConstraint(name = "UQ_api_usage_gst_type_period",
                columnNames = {"CompanyGSTId", "api_type", "period_key"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiUsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    /** EINVOICE | EWAYBILL | IMS */
    @Column(name = "api_type", length = 20, nullable = false)
    private String apiType;

    /** yyyy-MM, e.g. "2026-09" */
    @Column(name = "period_key", length = 7, nullable = false)
    private String periodKey;

    @Column(name = "call_count", nullable = false)
    @Builder.Default
    private Integer callCount = 0;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}
