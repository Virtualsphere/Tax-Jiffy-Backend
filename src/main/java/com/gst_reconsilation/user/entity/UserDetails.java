package com.gst_reconsilation.user.entity;

import com.gst_reconsilation.company.entity.CompanyProfile;
import com.gst_reconsilation.roles.entity.Roles;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "UserDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyId")
    private CompanyProfile company;

    @Column(name = "UserName", length = 255)
    private String userName;

    @Column(name = "UserEmail", length = 255)
    private String userEmail;

    @Column(name = "Mobile", length = 20)
    private String mobile;

    @Column(name = "UserPassword", columnDefinition = "TEXT")
    private String userPassword;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_super_admin", nullable = false)
    @Builder.Default
    private Boolean isSuperAdmin = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleId")
    private Roles role;

    /** Updated on WebSocket connect/disconnect — "last seen", not a heartbeat, so granularity is bounded by session length. */
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_date", nullable = false)
    @Builder.Default
    private LocalDate createdDate = LocalDate.now();

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "updated_by")
    private Integer updatedBy;
}