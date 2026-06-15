package com.gst_reconsilation.gstr1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "gstr1_docs",
        indexes = {
                @Index(name = "idx_docs_filing", columnList = "filing_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr1Docs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filing_id")
    private Gstr1Filing filing;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "nature_of_document", length = 255, nullable = false)
    private String natureOfDocument;

    @Column(name = "sr_no_from", length = 50)
    private String srNoFrom;

    @Column(name = "sr_no_to", length = 50)
    private String srNoTo;

    @Column(name = "total_number")
    @Builder.Default
    private Integer totalNumber = 0;

    @Column(name = "cancelled")
    @Builder.Default
    private Integer cancelled = 0;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();
}