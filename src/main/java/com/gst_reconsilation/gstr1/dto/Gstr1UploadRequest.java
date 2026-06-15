package com.gst_reconsilation.gstr1.dto;

import lombok.Data;
@Data
public class Gstr1UploadRequest {
    private Integer companyGstId;
    private String financialYear;
    private String taxPeriod;
}
