package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class HsnApiResponse {
    @JsonProperty("hsn") HsnBlock hsn;

    @Data public static class HsnBlock {
        @JsonProperty("hsn_b2b") List<HsnEntry> hsnB2b;
        @JsonProperty("hsn_b2c") List<HsnEntry> hsnB2c;
    }
    @Data public static class HsnEntry {
        @JsonProperty("hsn_sc")    String hsn;
        @JsonProperty("desc")      String description;
        @JsonProperty("user_desc") String userDesc;
        @JsonProperty("uqc")       String uqc;
        @JsonProperty("qty")       BigDecimal quantity;
        @JsonProperty("txval")     BigDecimal taxableValue;
        @JsonProperty("rt")        BigDecimal rate;
        @JsonProperty("iamt")      BigDecimal iamt;
        @JsonProperty("camt")      BigDecimal camt;
        @JsonProperty("samt")      BigDecimal samt;
        @JsonProperty("csamt")     BigDecimal csamt;
    }
}
