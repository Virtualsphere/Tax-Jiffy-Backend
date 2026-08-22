// gstr2b/dto/Gstr2bReconciliationRow.java
package com.gst_reconsilation.gstr2b.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One reconciled row: the uploaded GSTR-2B data (Gstr2Filing / Gstr2B2b — the same records
 * behind the "Purchase Register" upload) vs IMS (already-uploaded, for the same
 * companyGst + retperiod), joined on (supplierGstin, invoiceNumber). Computed fresh on
 * every request in Gstr2bReconciliationService — never persisted — so a correction to the
 * GSTR-2B side is reflected the next time this is fetched, with no separate re-sync step.
 */
@Data @Builder
public class Gstr2bReconciliationRow {
    /** Synthetic key: "gstr2bInvoiceId|imsInvoiceId", either half may be absent. */
    private String id;
    /** Id of the underlying Gstr2B2b row (the corrigible GSTR-2B side), null for an IMS-only row. */
    private Integer gstr2bInvoiceId;
    private Integer imsInvoiceId;

    private String supplierGstin;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String itcAvailability;

    /**
     * MATCHED | ROUNDING_DIFFERENCE | TAXABLE_VALUE_DIFFERS | TAX_AMOUNT_DIFFERS |
     * TAX_HEAD_DIFFERS_POS | ONLY_IN_2B | ONLY_IN_IMS
     */
    private String matchStatus;

    private BigDecimal gstr2bTaxableValue;
    private BigDecimal gstr2bIgst;
    private BigDecimal gstr2bCgst;
    private BigDecimal gstr2bSgst;
    private BigDecimal gstr2bCess;
    private BigDecimal gstr2bTotalTax;

    private BigDecimal imsTaxableValue;
    private BigDecimal imsIgst;
    private BigDecimal imsCgst;
    private BigDecimal imsSgst;
    private BigDecimal imsCess;
    private BigDecimal imsTotalTax;

    private BigDecimal deltaTaxable;
    private BigDecimal deltaIgst;
    private BigDecimal deltaCgst;
    private BigDecimal deltaSgst;
    private BigDecimal deltaCess;
    private BigDecimal deltaTotal;

    /** Sum of every head where GSTR-2B > IMS — credit claimed that IMS does not (yet) support. */
    private BigDecimal atRisk;
    /** Sum of every head where IMS > GSTR-2B — credit sitting in IMS that GSTR-2B hasn't picked up. */
    private BigDecimal unclaimed;

    private boolean edited;
    private String reconciliationAction;
    private String remarks;
}
