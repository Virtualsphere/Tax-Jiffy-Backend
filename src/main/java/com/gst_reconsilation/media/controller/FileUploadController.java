package com.gst_reconsilation.media.controller;

import com.gst_reconsilation.media.dto.FileUploadResponse;
import com.gst_reconsilation.media.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "Upload photos/documents to local storage and get back a public URL")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload a single photo",
            description = "Stores the image on local disk and returns the public URL along with file metadata.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        FileUploadResponse response = fileStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Upload multiple photos in one request",
            description = "Stores each image on local disk and returns a list of public URLs + metadata, in the same order as uploaded.")
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<FileUploadResponse> responses = fileStorageService.storeAll(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}