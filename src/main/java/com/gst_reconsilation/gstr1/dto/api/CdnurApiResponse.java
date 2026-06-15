package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class CdnurApiResponse {
    @JsonProperty("cdnur") List<CdnurEntry> cdnur;

    @Data public static class CdnurEntry {
        @JsonProperty("typ")     String urType;
        @JsonProperty("ntty")    String noteType;
        @JsonProperty("nt_num")  String noteNumber;
        @JsonProperty("nt_dt")   String noteDate;
        @JsonProperty("val")     BigDecimal noteValue;
        @JsonProperty("pos")     String pos;
        @JsonProperty("itms")    List<InvoiceItem> itms;
    }
}
