package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class InvoiceItem {
    @JsonProperty("num")      Integer num;
    @JsonProperty("itm_det") ItemDetail detail;
    // flat items (b2cs, at, etc.)
    @JsonProperty("rt")      BigDecimal rt;
    @JsonProperty("txval")   BigDecimal txval;
    @JsonProperty("iamt")    BigDecimal iamt;
    @JsonProperty("camt")    BigDecimal camt;
    @JsonProperty("samt")    BigDecimal samt;
    @JsonProperty("csamt")   BigDecimal csamt;
    @JsonProperty("ad_amt")  BigDecimal adAmt;
}
