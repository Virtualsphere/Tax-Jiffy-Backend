package com.gst_reconsilation.user.dto;

import lombok.Data;
@Data
public class UserGSTMappingRequest {
    private Integer userId;
    private Integer companyGstId;
    private Integer roleId;
    private Boolean isAdmin;
}
