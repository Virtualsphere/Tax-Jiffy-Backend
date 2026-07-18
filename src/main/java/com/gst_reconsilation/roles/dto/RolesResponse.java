package com.gst_reconsilation.roles.dto;

import lombok.Data;

@Data
public class RolesResponse {
    private Integer id;
    private String roleName;
    private String description;
    private Integer companyId;
    private String companyName;
    private Integer companyGstId;
    private String gstNumber;
    private Boolean isActive;
}