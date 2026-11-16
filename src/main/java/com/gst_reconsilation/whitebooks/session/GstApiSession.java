package com.gst_reconsilation.whitebooks.session;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One GSTN API session per CompanyGST. Created by the OTP request, activated by OTP verify.
 * The txn obtained here is sent as a header on every later GST API call for this GSTIN.
 */
@Entity
@Table(name = "gst_api_session",
        uniqueConstraints = @UniqueConstraint(name = "UQ_GstApiSession_CompanyGst", columnNames = "company_gst_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstApiSession {

    public static final String OTP_SENT = "OTP_SENT";
    public static final String ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_gst_id", nullable = false)
    private CompanyGST companyGST;

    @Column(name = "gstin", length = 15, nullable = false)
    private String gstin;

    /** GST portal username of the taxpayer (needed as the gst_username header). */
    @Column(name = "gst_username", length = 100, nullable = false)
    private String gstUsername;

    @Column(name = "state_cd", length = 2, nullable = false)
    private String stateCd;

    @Column(name = "txn", length = 100)
    private String txn;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "otp_requested_at")
    private LocalDateTime otpRequestedAt;

    @Column(name = "authenticated_at")
    private LocalDateTime authenticatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    public boolean isActive() {
        return ACTIVE.equals(status) && txn != null && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
