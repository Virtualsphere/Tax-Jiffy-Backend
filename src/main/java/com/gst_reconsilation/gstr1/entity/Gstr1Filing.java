package com.gst_reconsilation.gstr1.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_filings",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_gstr1_filings_Period",
                columnNames = {"CompanyGSTId", "FinancialYear", "TaxPeriod"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Filing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    @Column(name = "FinancialYear", length = 10, nullable = false)
    private String financialYear;

    @Column(name = "TaxPeriod", length = 20, nullable = false)
    private String taxPeriod;

    @Column(name = "FilingStatus", length = 20, nullable = false)
    @Builder.Default
    private String filingStatus = "DRAFT";

    // Path to the uploaded Excel file on disk
    @Column(name = "ExcelFilePath", length = 500)
    private String excelFilePath;

    @Column(name = "OriginalFileName", length = 255)
    private String originalFileName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "updated_by")
    private Integer updatedBy;
}