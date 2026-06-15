package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_eco",
        indexes = {
                @Index(name = "idx_eco_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Eco {

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

    @Column(name = "ecommerce_gstin", length = 15, nullable = false)
    private String ecommerceGstin;

    @Column(name = "ecommerce_operator_name", length = 255)
    private String ecommerceOperatorName;

    @Column(name = "net_value_of_supplies", precision = 18, scale = 2, nullable = false)
    private BigDecimal netValueOfSupplies;

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