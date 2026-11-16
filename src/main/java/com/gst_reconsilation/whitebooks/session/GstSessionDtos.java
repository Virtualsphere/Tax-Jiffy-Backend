package com.gst_reconsilation.whitebooks.session;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public final class GstSessionDtos {

    private GstSessionDtos() {}

    @Data
    public static class OtpRequest {
        /** GST portal username of the taxpayer. Optional on re-request: the last one used is reused. */
        private String gstUsername;
    }

    @Data
    public static class OtpVerifyRequest {
        private String otp;
    }

    @Data
    @Builder
    public static class SessionStatus {
        private Integer companyGstId;
        private String gstin;
        private String gstUsername;
        /** NONE, OTP_SENT, ACTIVE or EXPIRED */
        private String status;
        private boolean active;
        private LocalDateTime authenticatedAt;
        private LocalDateTime expiresAt;
    }
}
