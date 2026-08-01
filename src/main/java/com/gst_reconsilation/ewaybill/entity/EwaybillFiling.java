// ewaybill/entity/EwaybillFiling.java
package com.gst_reconsilation.ewaybill.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ewaybill_filings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EwaybillFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    /** DD/MM/YYYY — the date the sync was run against, since getewaybillsbydate is per-day. */
    @Column(name = "sync_date", length = 10, nullable = false)
    private String syncDate;

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