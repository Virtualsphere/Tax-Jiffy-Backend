// ims/dto/ImsUploadResponse.java
package com.gst_reconsilation.ims.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ImsUploadResponse {
    private Integer filingId;
    private String retPeriod;
    private String filingStatus;
    private int rowsImported;
}