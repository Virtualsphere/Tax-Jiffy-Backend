package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class NilApiResponse {
    @JsonProperty("nil") NilBlock nil;

    @Data public static class NilBlock {
        @JsonProperty("inv") List<NilEntry> inv;
    }
    @Data public static class NilEntry {
        @JsonProperty("sply_ty")   String supplyType;
        @JsonProperty("expt_amt")  BigDecimal exemptedAmount;
        @JsonProperty("nil_amt")   BigDecimal nilAmount;
        @JsonProperty("ngsup_amt") BigDecimal nonGstAmount;
    }
}
