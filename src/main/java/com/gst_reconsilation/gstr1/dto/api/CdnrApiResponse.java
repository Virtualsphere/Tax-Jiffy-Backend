package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CdnrApiResponse {
    @JsonProperty("cdnr") List<CdnrEntry> cdnr;

    @Data public static class CdnrEntry {
        @JsonProperty("ctin") String ctin;
        @JsonProperty("nt")   List<CdnrNote> nt;
    }
    @Data public static class CdnrNote {
        @JsonProperty("ntty")    String noteType;
        @JsonProperty("nt_num")  String noteNumber;
        @JsonProperty("nt_dt")   String noteDate;
        @JsonProperty("val")     BigDecimal noteValue;
        @JsonProperty("pos")     String pos;
        @JsonProperty("rchrg")   String reverseCharge;
        @JsonProperty("inv_typ") String invoiceType;
        @JsonProperty("itms")    List<InvoiceItem> itms;
    }
}