// gstr1/sync/dto/ApiCredentials.java
package com.gst_reconsilation.gstr1.dto;

import lombok.Data;

@Data
public class ApiCredentials {
    // Query params
    private String email;
    private String gstin;
    private String retperiod;
    // Headers
    private String gstUsername;
    private String stateCd;
    private String ipAddress;
    private String txn;
    private String clientId;
    private String clientSecret;
}