package com.gst_reconsilation.gstr1.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Gstr1SubmitResult {
    private boolean success;
    private Integer httpStatus;
    private String message;
    private String arn;
    private String rawResponse;
}