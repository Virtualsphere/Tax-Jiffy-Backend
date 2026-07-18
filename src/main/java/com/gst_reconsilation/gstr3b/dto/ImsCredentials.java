package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;

/**
 * Parameters for GET /ims/supplierinvoices (see API doc: "Get the section wise
 * supplier invoices in IMS based on rtn typ and section").
 * gstin / email / retperiod / section / rtnTyp are query params;
 * the rest are headers.
 */
@Data
public class ImsCredentials {
    // Query params
    private String gstin;
    private String email;
    private String retperiod;   // Return Period format MMYYYY
    private String section;     // Section Type
    private String rtnTyp;      // GSTR1/GSTR1A

    // Headers
    private String gstUsername;
    private String stateCd;
    private String ipAddress;
    private String txn;
    private String clientId;
    private String clientSecret;
}