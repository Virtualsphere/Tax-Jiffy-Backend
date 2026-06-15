package com.gst_reconsilation.roles.dto;

import lombok.Data;

@Data
public class RolesResponse {
    private Integer id;
    private String roleName;
    private String description;
    private Boolean isActive;
}
