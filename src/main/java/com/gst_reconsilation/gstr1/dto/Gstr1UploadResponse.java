package com.gst_reconsilation.gstr1.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr1UploadResponse {
    private Integer filingId;
    private String financialYear;
    private String taxPeriod;
    private String filingStatus;
    private String excelFilePath;
    private int totalRowsImported;

    // Per-sheet counts
    private int b2bRows;
    private int b2baRows;
    private int b2clRows;
    private int b2claRows;
    private int b2csRows;
    private int b2csaRows;
    private int cdnrRows;
    private int cdnraRows;
    private int cdnurRows;
    private int cdnuraRows;
    private int expRows;
    private int expaRows;
    private int atRows;
    private int ataRows;
    private int atadjRows;
    private int atadjаRows;
    private int exempRows;
    private int hsnB2bRows;
    private int hsnB2cRows;
    private int docsRows;
    private int ecoRows;
    private int ecoaRows;
    private int ecoB2bRows;
    private int ecoUrp2bRows;
    private int ecoB2cRows;
    private int ecoUrp2cRows;
    private int ecoAB2bRows;
    private int ecoAB2cRows;
    private int ecoAUrp2bRows;
    private int ecoAUrp2cRows;
}