package com.gst_reconsilation.apiusage.exception;

/** Thrown by ApiUsageService.recordAndEnforce() when a GST number has hit its 3rd-party API call limit for the period. */
public class ApiUsageLimitExceededException extends RuntimeException {
    public ApiUsageLimitExceededException(String message) {
        super(message);
    }
}
