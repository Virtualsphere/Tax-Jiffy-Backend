package com.gst_reconsilation.rolesmapping.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.roles.entity.Roles;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "RoleMapping")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Getter
@Setter
public class RoleMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "page_number", length = 255)
    private String pageNumber;

    @Column(name = "screen_number", length = 255)
    private String screenNumber;

    @Column(name = "add", nullable = false)
    @Builder.Default
    private Boolean add= true;

    @Column(name = "edit", nullable = false)
    @Builder.Default
    private Boolean edit= true;

    @Column(name = "view", nullable = false)
    @Builder.Default
    private Boolean view= true;

    @Column(name = "delete", nullable = false)
    @Builder.Default
    private Boolean delete= true;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleId")
    private Roles role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyId", nullable = false)
    private CompanyProfile company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;
}
