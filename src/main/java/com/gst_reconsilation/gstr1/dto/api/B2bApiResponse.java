package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class B2bApiResponse {
    @JsonProperty("b2b")
    List<B2bEntry> b2b;

    @Data public static class B2bEntry {
        @JsonProperty("ctin") String ctin;
        @JsonProperty("inv")  List<B2bInvoice> inv;
    }
    @Data public static class B2bInvoice {
        @JsonProperty("inum")     String invoiceNumber;
        @JsonProperty("idt")      String invoiceDate;
        @JsonProperty("val")      BigDecimal invoiceValue;
        @JsonProperty("pos")      String pos;
        @JsonProperty("rchrg")    String reverseCharge;
        @JsonProperty("inv_typ")  String invoiceType;
        @JsonProperty("etin")     String etin;
        @JsonProperty("diff_percent") BigDecimal diffPercent;
        @JsonProperty("itms")     List<InvoiceItem> itms;
    }
}
