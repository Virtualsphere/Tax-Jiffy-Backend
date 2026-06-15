package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_at",
        indexes = {
                @Index(name = "idx_at_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1At {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "place_of_supply", length = 50, nullable = false)
    private String placeOfSupply;

    @Column(name = "applicable_tax_rate_pct", precision = 5, scale = 2)
    private BigDecimal applicableTaxRatePct;

    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "gross_advance_received", precision = 18, scale = 2, nullable = false)
    private BigDecimal grossAdvanceReceived;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}