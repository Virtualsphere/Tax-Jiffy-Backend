package com.gst_reconsilation.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public final class OtpUsageDtos {

    private OtpUsageDtos() {}

    /** One GSTIN's OTP activity in a month, plus its current GST portal session. */
    @Data @Builder
    public static class Summary {
        private Integer companyGstId;
        private String gstNumber;
        private String companyName;
        private String periodKey;
        private long otpRequests;
        private long failedRequests;
        private long verifications;
        private long failedVerifications;
        private LocalDateTime lastOtpAt;
        /** NONE | OTP_SENT | ACTIVE | EXPIRED */
        private String sessionStatus;
        private LocalDateTime sessionExpiresAt;
    }

    @Data @Builder
    public static class Event {
        private Integer id;
        private LocalDateTime createdAt;
        /** REQUEST | VERIFY */
        private String action;
        private boolean success;
        private String message;
        private Integer userId;
        private String userName;
        private String userEmail;
    }
}
