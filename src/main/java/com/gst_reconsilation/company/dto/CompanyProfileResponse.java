package com.gst_reconsilation.company.dto;

import lombok.Data;
@Data
public class CompanyProfileResponse {
    private Integer id;
    private String companyName;
    private String companyLogo;
    private Boolean isActive;
}
