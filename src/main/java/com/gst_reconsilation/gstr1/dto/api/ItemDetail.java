package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

// ── Shared ────────────────────────────────────────────────────────

@Data
public class ItemDetail {
    @JsonProperty("rt")   BigDecimal rate;
    @JsonProperty("txval") BigDecimal taxableValue;
    @JsonProperty("iamt") BigDecimal iamt;
    @JsonProperty("camt") BigDecimal camt;
    @JsonProperty("samt") BigDecimal samt;
    @JsonProperty("csamt") BigDecimal csamt;
    @JsonProperty("ad_amt") BigDecimal advanceAmount;
}
