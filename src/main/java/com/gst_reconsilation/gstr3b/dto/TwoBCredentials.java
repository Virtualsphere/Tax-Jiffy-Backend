package com.gst_reconsilation.gstr3b.dto;

import lombok.Data;

/**
 * Optional body for POST /api/gstr3b/filings/{id}/sync-2b (Whitebooks GET /gstr2b/all).
 * GSTIN comes from the filing; keys, username and txn come from the server-side GST session.
 */
@Data
public class TwoBCredentials {
    /** Return period MMYYYY. Defaults to the filing's own period when omitted. */
    private String rtnprd;
    /** File number, only needed when GSTN splits a large 2B into several files. */
    private String filenum;
}
