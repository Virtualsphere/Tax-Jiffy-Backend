// einvoice/dto/api/EinvoiceHsnApiResponse.java
package com.gst_reconsilation.einvoice.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EinvoiceHsnApiResponse {
    private String gstin;
    private String fp;
    private HsnBlock hsn;

    @Data
    public static class HsnBlock {
        @JsonProperty("hsn_b2b") private List<HsnEntry> hsnB2b;
        @JsonProperty("hsn_b2c") private List<HsnEntry> hsnB2c;
    }

    @Data
    public static class HsnEntry {
        private Integer num;
        @JsonProperty("hsn_sc") private String hsnSc;
        private String desc;
        @JsonProperty("user_desc") private String userDesc;
        private String uqc;
        private BigDecimal qty;
        private BigDecimal rt;
        private BigDecimal txval;
        private BigDecimal iamt;
        private BigDecimal camt;
        private BigDecimal samt;
        private BigDecimal csamt;
        private String chksum;
    }
}