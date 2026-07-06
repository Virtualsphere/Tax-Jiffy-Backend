package com.gst_reconsilation.company.dto;

import lombok.Data;
import java.time.LocalDateTime;
@Data
public class CompanyGSTRequest {
    private Integer companyId;
    private String gstNumber;
}
