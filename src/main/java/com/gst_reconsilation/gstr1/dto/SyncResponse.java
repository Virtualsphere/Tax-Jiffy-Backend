// gstr1/sync/dto/SyncResponse.java
package com.gst_reconsilation.gstr1.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncResponse {
    private Integer filingId;
    private String financialYear;
    private String taxPeriod;
    private String status;
    private int totalRowsSynced;
    // Per-sheet counts
    private int b2bRows;
    private int b2csRows;
    private int b2csaRows;
    private int b2claRows;
    private int cdnrRows;
    private int cdnurRows;
    private int cdnuraRows;
    private int expRows;
    private int expaRows;
    private int atRows;
    private int ataRows;  // txp maps to ata
    private int exempRows;
    private int hsnB2bRows;
    private int hsnB2cRows;
}