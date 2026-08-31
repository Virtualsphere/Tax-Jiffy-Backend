package com.gst_reconsilation.user.dto;

import lombok.Data;
@Data
public class UserRequest {
    private Integer companyGstId;
    private String userName;
    private String userEmail;
    private String userPassword;
    private String mobile;
}
