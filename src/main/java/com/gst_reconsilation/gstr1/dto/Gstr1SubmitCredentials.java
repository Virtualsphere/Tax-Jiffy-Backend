package com.gst_reconsilation.gstr1.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Parameters for PUT /gstr1/retsave ("Used to save entire GSTR1 invoices").
 * email is a query param; gstin, ret_period and the rest are headers.
 *
 * grossTurnover / currentGrossTurnover map to the payload's top-level "gt" /
 * "cur_gt" fields. Neither value is tracked anywhere in the current schema
 * (no company turnover profile table), so they're supplied here at submit
 * time rather than looked up - default to zero if omitted. TODO: once a
 * company turnover profile exists, prefer that over requiring manual entry
 * on every submission.
 */
@Data
public class Gstr1SubmitCredentials {
    // Query param
    private String email;

    // Headers
    private String gstin;
    private String retPeriod; // header "ret_period", format MMYYYY
    private String gstUsername;
    private String stateCd;
    private String ipAddress;
    private String txn;
    private String clientId;
    private String clientSecret;

    // Payload fields not tracked elsewhere - see class Javadoc
    private BigDecimal grossTurnover;
    private BigDecimal currentGrossTurnover;
}