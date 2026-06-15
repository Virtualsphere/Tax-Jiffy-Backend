package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class B2claApiResponse {
    @JsonProperty("b2cla") List<B2claEntry> b2cla;

    @Data public static class B2claEntry {
        @JsonProperty("pos")      String pos;
        @JsonProperty("inv")      List<B2claInvoice> inv;
    }
    @Data public static class B2claInvoice {
        @JsonProperty("oinum")    String originalInvoiceNumber;
        @JsonProperty("oidt")     String originalInvoiceDate;
        @JsonProperty("inum")     String revisedInvoiceNumber;
        @JsonProperty("idt")      String revisedInvoiceDate;
        @JsonProperty("val")      BigDecimal invoiceValue;
        @JsonProperty("etin")     String etin;
        @JsonProperty("diff_percent") BigDecimal diffPercent;
        @JsonProperty("itms")     List<InvoiceItem> itms;
    }
}
