package com.gst_reconsilation.gstr1.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class ExpApiResponse {
    @JsonProperty("exp") List<ExpEntry> exp;

    @Data public static class ExpEntry {
        @JsonProperty("exp_typ") String exportType;
        @JsonProperty("inv")     List<ExpInvoice> inv;
    }
    @Data public static class ExpInvoice {
        @JsonProperty("inum")         String invoiceNumber;
        @JsonProperty("idt")          String invoiceDate;
        @JsonProperty("val")          BigDecimal invoiceValue;
        @JsonProperty("sbpcode")      String portCode;
        @JsonProperty("sbnum")        String shippingBillNumber;
        @JsonProperty("sbdt")         String shippingBillDate;
        @JsonProperty("diff_percent") BigDecimal diffPercent;
        @JsonProperty("itms")         List<InvoiceItem> itms;
    }
}
