package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_exemp",
        indexes = {
                @Index(name = "idx_exemp_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Exemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "nil_rated_supplies", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal nilRatedSupplies = BigDecimal.ZERO;

    @Column(name = "exempted_supplies", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal exemptedSupplies = BigDecimal.ZERO;

    @Column(name = "non_gst_supplies", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal nonGstSupplies = BigDecimal.ZERO;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}