// config/GstApiCredentialsProperties.java
package com.gst_reconsilation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Whitebooks settings. Whitebooks issues a separate client id/secret per product
 * (GST, e-Way Bill, e-Invoice), so each product has its own key pair here.
 * The GST username/state are NOT configured globally: they belong to each taxpayer and
 * are stored on the per-GSTIN session (see GstAuthService).
 */
@Component
@ConfigurationProperties(prefix = "gst.api")
@Data
public class GstApiCredentialsProperties {
    /** Email of the Whitebooks account that owns the keys. */
    private String email;
    private String ipAddress;

    /** GST APIs (GSTR-1, GSTR-2B, IMS, GST e-invoice fetch). */
    private String baseUrl;
    private String clientId;
    private String clientSecret;

    /** How long a verified GSTN session is reused before asking for a new OTP. */
    private int sessionMinutes = 360;

    private String einvoiceBaseUrl;
    private String imsBaseUrl;

    /** e-Way Bill APIs use their own key pair. */
    private String ewaybillBaseUrl;
    private String ewaybillClientId;
    private String ewaybillClientSecret;
}
