package com.gst_reconsilation.user.dto;

import lombok.Data;
@Data
public class UserRequest {
    private Integer companyGstId;
    private String userName;
    private String userEmail;
    private String userPassword;
    private String mobile;
    /** Role on companyGstId for a new sub-user; defaults to the GST's USER role. */
    private Integer roleId;
}
