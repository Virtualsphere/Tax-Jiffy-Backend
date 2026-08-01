// einvoice/dto/api/EinvoiceIrnDtlApiResponse.java
package com.gst_reconsilation.einvoice.dto.api;

import lombok.Data;

@Data
public class EinvoiceIrnDtlApiResponse {
    private DataBlock data;

    @Data
    public static class DataBlock {
        private Long ackNo;
        private String ackDt;
        private String irn;
        private String signedInvoice;
        private String signedQrCode;
        private String status;
        private Long ewbNo;
        private String ewbDt;
        private String ewbValidTill;
        private String remarks;
        private String cnldt;
        private String cnlRsn;
        private String cnlRem;
    }
}