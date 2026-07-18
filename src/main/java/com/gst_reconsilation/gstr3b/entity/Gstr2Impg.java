package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_impg",
        indexes = {
                @Index(name = "idx_gstr2_impg_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2Impg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr2Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "port_code", length = 20)
    private String portCode;

    @Column(name = "bill_of_entry_number", length = 50, nullable = false)
    private String billOfEntryNumber;

    @Column(name = "bill_of_entry_date")
    private LocalDate billOfEntryDate;

    @Column(name = "bill_of_entry_value", precision = 18, scale = 2)
    private BigDecimal billOfEntryValue;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "gstin_of_sez_supplier", length = 15)
    private String gstinOfSezSupplier;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "integrated_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTaxPaid = BigDecimal.ZERO;

    @Column(name = "cess_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessPaid = BigDecimal.ZERO;

    @Column(name = "eligibility_for_itc", length = 50)
    private String eligibilityForItc;

    @Column(name = "availed_itc_integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcIntegratedTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcCess = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}