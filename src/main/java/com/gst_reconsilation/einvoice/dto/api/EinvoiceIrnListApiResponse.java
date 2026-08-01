// einvoice/dto/api/EinvoiceIrnListApiResponse.java
package com.gst_reconsilation.einvoice.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EinvoiceIrnListApiResponse {
    @JsonProperty("irnList") private List<IrnBlock> irnList;
    private String requestDate;

    @Data
    public static class IrnBlock {
        private String ctin;
        @JsonProperty("irnDtl") private List<IrnEntry> irnDtl;
    }

    @Data
    public static class IrnEntry {
        private String docNum;
        private String docDt;
        private String docType;
        private String supplyType;
        private BigDecimal totInvAmt;
        private String irn;
        private String irnStatus;
        private Long ackNo;
        private String ackDt;
        private Long ewbNo;
        private String ewbDt;
        private String cnldt;
    }
}