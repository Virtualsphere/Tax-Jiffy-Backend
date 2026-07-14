package com.gst_reconsilation.rolesmapping.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RoleMappingResponse {
    private Integer id;
    private Integer roleId;
    private String roleName;
    private Integer companyId;
    private String companyName;
    private Integer companyGstId;
    private String gstNumber;
    private String pageNumber;
    private String screenNumber;
    private Boolean add;
    private Boolean edit;
    private Boolean view;
    private Boolean delete;
    private LocalDate createdDate;
}