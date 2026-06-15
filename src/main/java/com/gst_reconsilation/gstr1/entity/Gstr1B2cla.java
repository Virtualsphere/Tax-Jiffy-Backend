package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_b2cla",
        indexes = {
                @Index(name = "idx_b2cla_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1B2cla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "original_invoice_number", length = 50, nullable = false)
    private String originalInvoiceNumber;

    @Column(name = "original_invoice_date", nullable = false)
    private LocalDate originalInvoiceDate;

    @Column(name = "original_place_of_supply", length = 50, nullable = false)
    private String originalPlaceOfSupply;

    @Column(name = "revised_invoice_number", length = 50, nullable = false)
    private String revisedInvoiceNumber;

    @Column(name = "revised_invoice_date", nullable = false)
    private LocalDate revisedInvoiceDate;

    @Column(name = "invoice_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal invoiceValue;

    @Column(name = "applicable_tax_rate_pct", precision = 5, scale = 2)
    private BigDecimal applicableTaxRatePct;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "ecommerce_gstin", length = 15)
    private String ecommerceGstin;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}