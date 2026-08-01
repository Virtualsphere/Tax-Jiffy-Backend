// ims/dto/ImsSyncResponse.java
package com.gst_reconsilation.ims.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ImsSyncResponse {
    private Integer filingId;
    private String retPeriod;
    private String syncStatus;
    private int rowsSynced;
}