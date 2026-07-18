package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;

/**
 * Parameters for GET /gstr2b/all (see API doc: "Getting the details for given
 * GSTIN, Return Period").
 * gstin / rtnprd / filenum / email are query params; the rest are headers.
 */
@Data
public class TwoBCredentials {
    // Query params
    private String gstin;
    private String rtnprd;   // Return Period
    private String filenum;  // File Number
    private String email;

    // Headers
    private String gstUsername;
    private String stateCd;
    private String ipAddress;
    private String txn;
    private String clientId;
    private String clientSecret;
}