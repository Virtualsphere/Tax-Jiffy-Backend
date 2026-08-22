// gstr2b/dto/Gstr2bInvoiceUpdateRequest.java
package com.gst_reconsilation.gstr2b.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Partial correction for one purchase-register (GSTR-2, table Gstr2B2b) invoice row —
 * this IS the uploaded GSTR-2B data referred to by the GSTR-2B page. Any field left null
 * is left untouched: the caller only sends what the user actually changed in the grid.
 */
@Data
public class Gstr2bInvoiceUpdateRequest {
    private BigDecimal taxableValue;
    private BigDecimal rate;
    private BigDecimal integratedTaxPaid;
    private BigDecimal centralTaxPaid;
    private BigDecimal stateUtTaxPaid;
    private BigDecimal cessPaid;
    /** ACCEPT / REJECT / PENDING */
    private String reconciliationAction;
    private String remarks;
}
