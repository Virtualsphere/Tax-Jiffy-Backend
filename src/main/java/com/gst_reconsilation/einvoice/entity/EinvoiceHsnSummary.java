// einvoice/entity/EinvoiceHsnSummary.java
package com.gst_reconsilation.einvoice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "einvoice_hsn_summary", indexes = @Index(name = "idx_hsn_filing", columnList = "filing_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EinvoiceHsnSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private EinvoiceFiling filing;

    /** B2B or B2C */
    @Column(name = "type", length = 5, nullable = false)
    private String type;

    @Column(name = "hsn_sc", length = 20)
    private String hsnSc;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "user_desc", length = 255)
    private String userDesc;

    @Column(name = "uqc", length = 20)
    private String uqc;

    @Column(name = "qty", precision = 18, scale = 3)
    private BigDecimal qty;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTax = BigDecimal.ZERO;

    @Column(name = "central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal centralTax = BigDecimal.ZERO;

    @Column(name = "state_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal stateTax = BigDecimal.ZERO;

    @Column(name = "cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}