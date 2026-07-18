package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_b2bur",
        indexes = {
                @Index(name = "idx_gstr2_b2bur_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2B2bur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr2Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "invoice_number", length = 50, nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_value", precision = 18, scale = 2)
    private BigDecimal invoiceValue;

    @Column(name = "place_of_supply", length = 50)
    private String placeOfSupply;

    @Column(name = "supply_type", length = 20)
    private String supplyType;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "integrated_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal integratedTaxPaid = BigDecimal.ZERO;

    @Column(name = "central_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal centralTaxPaid = BigDecimal.ZERO;

    @Column(name = "state_ut_tax_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal stateUtTaxPaid = BigDecimal.ZERO;

    @Column(name = "cess_paid", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessPaid = BigDecimal.ZERO;

    @Column(name = "eligibility_for_itc", length = 50)
    private String eligibilityForItc;

    @Column(name = "availed_itc_integrated_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcIntegratedTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_central_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcCentralTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_state_ut_tax", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcStateUtTax = BigDecimal.ZERO;

    @Column(name = "availed_itc_cess", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availedItcCess = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}