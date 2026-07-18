package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;

@Data
public class Gstr2UploadRequest {
    private Integer companyGstId;
    private String financialYear;
    private String taxPeriod;
}