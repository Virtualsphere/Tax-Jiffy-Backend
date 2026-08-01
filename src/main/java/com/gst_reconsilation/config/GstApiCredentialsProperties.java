// config/GstApiCredentialsProperties.java
package com.gst_reconsilation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gst.api")
@Data
public class GstApiCredentialsProperties {
    private String email;
    private String gstUsername;
    private String stateCd;
    private String ipAddress;
    private String clientId;
    private String clientSecret;
    private String einvoiceBaseUrl;
    private String ewaybillBaseUrl;
    private String imsBaseUrl;
}