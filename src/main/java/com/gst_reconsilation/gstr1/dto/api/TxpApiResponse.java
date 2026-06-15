package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class TxpApiResponse {
    @JsonProperty("txpd") List<TxpBlock> txpd;

    @Data public static class TxpBlock {
        @JsonProperty("txpd_b2b")  List<TxpEntry> txpdB2b;
        @JsonProperty("txpd_b2c")  List<TxpEntry> txpdB2c;
        @JsonProperty("txpd_exp")  List<TxpEntry> txpdExp;
        @JsonProperty("txpd_ecom") TxpEcom txpdEcom;
    }
    @Data public static class TxpEntry {
        @JsonProperty("pos")     String pos;
        @JsonProperty("sply_ty") String supplyType;
        @JsonProperty("itms")    List<InvoiceItem> itms;
    }
    @Data public static class TxpEcom {
        @JsonProperty("txpd_ecomurp2b") List<TxpEntry> ecomUrp2b;
        @JsonProperty("txpd_ecomurp2c") List<TxpEntry> ecomUrp2c;
    }
}
