package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Gstr3bInterestLateFeeRequest {
    private BigDecimal interestIntegratedTax;
    private BigDecimal interestCentralTax;
    private BigDecimal interestStateUtTax;
    private BigDecimal interestCess;
    private BigDecimal lateFeeCentralTax;
    private BigDecimal lateFeeStateUtTax;
}