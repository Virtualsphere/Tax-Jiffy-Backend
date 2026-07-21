package com.gst_reconsilation.media;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Exposes {@code app.upload.dir} on disk as static content under the
 * {@code /uploads/photos/**} URL path, so a file saved by
 * {@link com.gst_reconsilation.media.service.FileStorageService} is immediately
 * reachable at the URL returned from the upload endpoint.
 */
@Configuration
@RequiredArgsConstructor
public class FileWebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = fileStorageProperties.getDir();
        // file: location must end with a separator
        String location = "file:" + dir + (dir.endsWith(File.separator) ? "" : File.separator);

        registry.addResourceHandler("/uploads/photos/**")
                .addResourceLocations(location);
    }
}