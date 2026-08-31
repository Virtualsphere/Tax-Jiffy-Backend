package com.gst_reconsilation.admin.dto;

import lombok.Data;

@Data
public class ApiUsageOverrideRequest {
    /** Null clears the override, reverting to the subscription plan's default limit. */
    private Integer apiCallLimitOverride;
}
