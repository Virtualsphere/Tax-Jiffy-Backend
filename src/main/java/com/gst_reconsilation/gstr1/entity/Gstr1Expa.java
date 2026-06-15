package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_expa",
        indexes = {
                @Index(name = "idx_expa_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Expa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "export_type", length = 20)
    private String exportType;

    @Column(name = "original_invoice_number", length = 50, nullable = false)
    private String originalInvoiceNumber;

    @Column(name = "original_invoice_date", nullable = false)
    private LocalDate originalInvoiceDate;

    @Column(name = "revised_invoice_number", length = 50, nullable = false)
    private String revisedInvoiceNumber;

    @Column(name = "revised_invoice_date", nullable = false)
    private LocalDate revisedInvoiceDate;

    @Column(name = "invoice_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal invoiceValue;

    @Column(name = "port_code", length = 10)
    private String portCode;

    @Column(name = "shipping_bill_number", length = 50)
    private String shippingBillNumber;

    @Column(name = "shipping_bill_date")
    private LocalDate shippingBillDate;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}