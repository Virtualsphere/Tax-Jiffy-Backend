package com.gst_reconsilation.user.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
