package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class B2csApiResponse {
    @JsonProperty("b2cs") List<B2csEntry> b2cs;

    @Data public static class B2csEntry {
        @JsonProperty("typ")      String type;
        @JsonProperty("pos")      String pos;
        @JsonProperty("rt")       BigDecimal rate;
        @JsonProperty("txval")    BigDecimal txval;
        @JsonProperty("iamt")     BigDecimal iamt;
        @JsonProperty("camt")     BigDecimal camt;
        @JsonProperty("samt")     BigDecimal samt;
        @JsonProperty("csamt")    BigDecimal csamt;
        @JsonProperty("etin")     String etin;
        @JsonProperty("sply_ty")  String supplyType;
        @JsonProperty("diff_percent") BigDecimal diffPercent;
    }
}