package com.gst_reconsilation.apiusage.entity;

import com.gst_reconsilation.company.entity.CompanyGST;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One GST-portal OTP action through Whitebooks: an OTP request (GSTN sends an SMS) or an OTP
 * verification. Logged for monitoring only; OTP calls do not count towards the API limit.
 */
@Entity
@Table(name = "gst_otp_events", indexes = {
        @Index(name = "IX_gst_otp_events_gst_time", columnList = "CompanyGSTId, created_at"),
        @Index(name = "IX_gst_otp_events_time", columnList = "created_at")})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GstOtpEvent {

    public static final String REQUEST = "REQUEST";
    public static final String VERIFY = "VERIFY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CompanyGSTId", nullable = false)
    private CompanyGST companyGST;

    @Column(name = "gstin", length = 15)
    private String gstin;

    /** App user who clicked "Send OTP" / "Verify" */
    @Column(name = "user_id")
    private Integer userId;

    /** REQUEST | VERIFY */
    @Column(name = "action", length = 10, nullable = false)
    private String action;

    @Column(name = "success", nullable = false)
    private boolean success;

    /** GST portal / Whitebooks error message when success = false */
    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
