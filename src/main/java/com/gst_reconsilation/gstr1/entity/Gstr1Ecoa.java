package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_ecoa",
        indexes = {
                @Index(name = "idx_ecoa_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Ecoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "nature_of_supply", length = 100)
    private String natureOfSupply;

    @Column(name = "financial_year", length = 10, nullable = false)
    private String financialYear;

    @Column(name = "original_month_quarter", length = 20, nullable = false)
    private String originalMonthQuarter;

    @Column(name = "original_ecommerce_gstin", length = 15, nullable = false)
    private String originalEcommerceGstin;

    @Column(name = "revised_ecommerce_gstin", length = 15)
    private String revisedEcommerceGstin;

    @Column(name = "ecommerce_operator_name", length = 255)
    private String ecommerceOperatorName;

    @Column(name = "revised_net_value_of_supplies", precision = 18, scale = 2, nullable = false)
    private BigDecimal revisedNetValueOfSupplies;

    @Column(name = "integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTax = BigDecimal.ZERO;

    @Column(name = "central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal centralTax = BigDecimal.ZERO;

    @Column(name = "state_ut_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal stateUtTax = BigDecimal.ZERO;

    @Column(name = "cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cess = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}