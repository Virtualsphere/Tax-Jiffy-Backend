package com.gst_reconsilation.whitebooks.session;

/**
 * No active GSTN session for this GSTIN. Mapped to HTTP 428 so the frontend can open the
 * "Connect to GST portal" (OTP) dialog instead of treating it as a login/permission error.
 */
public class GstSessionRequiredException extends RuntimeException {
    public GstSessionRequiredException(String message) {
        super(message);
    }
}
