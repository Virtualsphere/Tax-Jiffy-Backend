package com.gst_reconsilation.gstr1.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Body for POST /api/gstr1/filings/{id}/submit (Whitebooks PUT /gstr1/retsave).
 *
 * GST API keys, username and txn are no longer accepted from the client: the server uses
 * its own Whitebooks keys and the per-GSTIN session from /api/gst-session/{companyGstId}.
 * GSTIN and return period come from the filing itself.
 *
 * grossTurnover / currentGrossTurnover map to the payload's top-level "gt" / "cur_gt"
 * fields. Neither is tracked anywhere in the current schema, so they're supplied here at
 * submit time (default zero). TODO: move to a company turnover profile.
 */
@Data
public class Gstr1SubmitCredentials {
    private BigDecimal grossTurnover;
    private BigDecimal currentGrossTurnover;
}
