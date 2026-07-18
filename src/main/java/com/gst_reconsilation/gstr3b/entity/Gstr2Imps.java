package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_imps",
        indexes = {
                @Index(name = "idx_gstr2_imps_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2Imps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr2Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "invoice_number_of_reg_recipient", length = 50, nullable = false)
    private String invoiceNumberOfRegRecipient;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "invoice_value", precision = 18, scale = 2)
    private BigDecimal invoiceValue;

    @Column(name = "place_of_supply", length = 50)
    private String placeOfSupply;

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