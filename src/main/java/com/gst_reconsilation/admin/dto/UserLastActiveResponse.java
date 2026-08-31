package com.gst_reconsilation.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class UserLastActiveResponse {
    private Integer userId;
    private String userName;
    private String userEmail;
    private String companyName;
    private boolean online;
    private LocalDateTime lastActiveAt;
}
