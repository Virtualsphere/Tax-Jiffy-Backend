package com.gst_reconsilation.media;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the `app.upload.*` properties from application.yml.
 *
 *   app:
 *     upload:
 *       dir: /home/aditya/uploads/photos
 *       base-url: http://localhost:7071/uploads/photos
 */
@Configuration
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class FileStorageProperties {

    /** Absolute directory on disk where files are physically written. */
    private String dir;

    /** Public URL prefix under which the same files are served (see FileWebConfig). */
    private String baseUrl;
}