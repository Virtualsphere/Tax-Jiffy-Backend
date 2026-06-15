package com.gst_reconsilation.company.dto;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class CompanyGSTResponse {
    private Integer id;
    private String gstNumber;
    private Integer companyId;
    private String companyName;
    private String subscriptionPlanName;
    private Boolean isPaymentDone;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
}
