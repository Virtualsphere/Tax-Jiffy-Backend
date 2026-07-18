package com.gst_reconsilation.gstr3b.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr2UploadResponse {
    private Integer filingId;
    private String financialYear;
    private String taxPeriod;
    private String filingStatus;
    private String excelFilePath;
    private int totalRowsImported;

    // Per-sheet counts
    private int b2bRows;
    private int b2burRows;
    private int impsRows;
    private int impgRows;
    private int cdnrRows;
    private int cdnurRows;
    private int atRows;
    private int atadjRows;
    private int exempRows;
    private int itcrRows;
    private int hsnSumRows;
}