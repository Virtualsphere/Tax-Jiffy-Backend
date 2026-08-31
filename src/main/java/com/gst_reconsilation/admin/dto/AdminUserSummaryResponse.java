package com.gst_reconsilation.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class AdminUserSummaryResponse {
    private Integer id;
    private String userName;
    private String userEmail;
    private String mobile;
    private Integer companyId;
    private String companyName;
    private Boolean isActive;
    private Boolean isSuperAdmin;
    private LocalDate createdDate;
    private List<AdminUserGstSummary> gstMemberships;
}
