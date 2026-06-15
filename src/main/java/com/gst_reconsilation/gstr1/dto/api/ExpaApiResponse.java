package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class ExpaApiResponse {
    @JsonProperty("expa") List<ExpaEntry> expa;

    @Data public static class ExpaEntry {
        @JsonProperty("exp_typ") String exportType;
        @JsonProperty("inv")     List<ExpaInvoice> inv;
    }
    @Data public static class ExpaInvoice {
        @JsonProperty("oinum")        String originalInvoiceNumber;
        @JsonProperty("oidt")         String originalInvoiceDate;
        @JsonProperty("inum")         String revisedInvoiceNumber;
        @JsonProperty("idt")          String revisedInvoiceDate;
        @JsonProperty("val")          BigDecimal invoiceValue;
        @JsonProperty("sbpcode")      String portCode;
        @JsonProperty("sbnum")        String shippingBillNumber;
        @JsonProperty("sbdt")         String shippingBillDate;
        @JsonProperty("itms")         List<InvoiceItem> itms;
    }
}
