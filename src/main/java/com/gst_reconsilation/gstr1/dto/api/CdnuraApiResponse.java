package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class CdnuraApiResponse {
    @JsonProperty("cdnura") List<CdnuraEntry> cdnura;

    @Data public static class CdnuraEntry {
        @JsonProperty("ont_num") String originalNoteNumber;
        @JsonProperty("ont_dt")  String originalNoteDate;
        @JsonProperty("nt_num")  String revisedNoteNumber;
        @JsonProperty("nt_dt")   String revisedNoteDate;
        @JsonProperty("ntty")    String noteType;
        @JsonProperty("typ")     String urType;
        @JsonProperty("val")     BigDecimal noteValue;
        @JsonProperty("pos")     String pos;
        @JsonProperty("itms")    List<InvoiceItem> itms;
    }
}