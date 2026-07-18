package com.gst_reconsilation.gstr3b.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr2_at",
        indexes = {
                @Index(name = "idx_gstr2_at_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr2At {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr2Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "place_of_supply", length = 50, nullable = false)
    private String placeOfSupply;

    @Column(name = "supply_type", length = 20)
    private String supplyType;

    @Column(name = "gross_advance_paid", precision = 18, scale = 2, nullable = false)
    private BigDecimal grossAdvancePaid;

    @Column(name = "cess_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}