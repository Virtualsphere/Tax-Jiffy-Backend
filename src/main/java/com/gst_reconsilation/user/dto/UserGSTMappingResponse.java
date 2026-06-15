package com.gst_reconsilation.user.dto;

import lombok.Data;
@Data
public class UserGSTMappingResponse {
    private Integer id;
    private Integer userId;
    private String userName;
    private Integer companyGstId;
    private String gstNumber;
    private String roleName;
    private Boolean isAdmin;
    private Boolean isActive;
}
