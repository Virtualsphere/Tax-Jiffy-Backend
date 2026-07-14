package com.gst_reconsilation.rolesmapping.dto;

import lombok.Data;

@Data
public class RoleMappingRequest {
    private Integer roleId;
    private Integer companyId;
    private Integer companyGstId;
    private String pageNumber;
    private String screenNumber;
    private Boolean add;
    private Boolean edit;
    private Boolean view;
    private Boolean delete;
}