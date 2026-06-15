package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class AtApiResponse {
    @JsonProperty("at") List<AtBlock> at;

    @Data public static class AtBlock {
        @JsonProperty("at_b2b")  List<AtEntry> atB2b;
        @JsonProperty("at_b2c")  List<AtEntry> atB2c;
        @JsonProperty("at_exp")  List<AtEntry> atExp;
        @JsonProperty("at_ecom") AtEcom atEcom;
    }
    @Data public static class AtEntry {
        @JsonProperty("pos")     String pos;
        @JsonProperty("sply_ty") String supplyType;
        @JsonProperty("itms")    List<InvoiceItem> itms;
    }
    @Data public static class AtEcom {
        @JsonProperty("at_ecomurp2b") List<AtEntry> ecomUrp2b;
        @JsonProperty("at_ecomurp2c") List<AtEntry> ecomUrp2c;
    }
}
