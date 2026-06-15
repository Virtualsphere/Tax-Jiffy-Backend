package com.gst_reconsilation.user.dto;

import lombok.Data;
@Data
public class UserResponse {
    private Integer id;
    private String userName;
    private String userEmail;
    private Integer companyId;
    private String companyName;
    private Boolean isActive;
}
