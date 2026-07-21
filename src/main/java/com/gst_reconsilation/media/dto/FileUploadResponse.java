package com.gst_reconsilation.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /** Publicly accessible URL to fetch the stored photo. */
    private String url;

    /** Generated filename actually stored on disk (unique, collision-safe). */
    private String fileName;

    /** Original filename the client uploaded, kept for reference/display. */
    private String originalFileName;

    /** MIME type of the uploaded file. */
    private String contentType;

    /** Size of the uploaded file in bytes. */
    private long sizeBytes;
}