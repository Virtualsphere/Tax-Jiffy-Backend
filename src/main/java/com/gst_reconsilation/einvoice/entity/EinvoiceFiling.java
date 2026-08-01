// einvoice/entity/EinvoiceFiling.java
package com.gst_reconsilation.einvoice.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "einvoice_filings",
        uniqueConstraints = @UniqueConstraint(name = "UQ_einvoice_filing_period",
                columnNames = {"CompanyGSTId", "RetPeriod"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EinvoiceFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    /** MMYYYY, e.g. "102025" */
    @Column(name = "RetPeriod", length = 6, nullable = false)
    private String retPeriod;

    @Column(name = "sync_status", length = 20, nullable = false)
    @Builder.Default
    private String syncStatus = "PENDING";

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;
}