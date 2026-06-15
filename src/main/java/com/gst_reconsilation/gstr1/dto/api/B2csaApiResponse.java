package com.gst_reconsilation.gstr1.dto.api;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class B2csaApiResponse {
    @JsonProperty("b2csa") List<B2csaEntry> b2csa;

    @Data public static class B2csaEntry {
        @JsonProperty("pos")      String pos;
        @JsonProperty("typ")      String type;
        @JsonProperty("omon")     String originalMonth;  // "072017" → financialYear+month
        @JsonProperty("sply_ty")  String supplyType;
        @JsonProperty("itms")     List<InvoiceItem> itms;
    }
}
