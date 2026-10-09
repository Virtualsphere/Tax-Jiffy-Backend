package com.gst_reconsilation.permission;

/** Mapped to HTTP 403 by GlobalExceptionHandler. */
public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
