package com.gst_reconsilation.media.service;

import com.gst_reconsilation.media.FileStorageProperties;
import com.gst_reconsilation.media.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    /** Only image content-types are accepted by this endpoint. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "heic", "heif"
    );

    private Path rootLocation;

    /** Resolves and creates the storage directory once at startup, failing fast if it can't be created. */
    @jakarta.annotation.PostConstruct
    public void init() {
        this.rootLocation = Paths.get(fileStorageProperties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File upload storage directory ready at {}", rootLocation);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory: " + rootLocation, e);
        }
    }

    /**
     * Validates and saves an uploaded photo to disk, returning the metadata + public URL
     * the caller needs to display or reference it later.
     */
    public FileUploadResponse store(MultipartFile file) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path destination = rootLocation.resolve(storedFileName).normalize();
        // Defense in depth against path traversal, even though we control the generated name.
        if (!destination.getParent().equals(rootLocation)) {
            throw new IllegalArgumentException("Invalid file destination.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file: " + storedFileName, e);
        }

        String url = StringUtils.trimTrailingCharacter(fileStorageProperties.getBaseUrl(), '/')
                + "/" + storedFileName;

        log.info("Stored uploaded photo '{}' ({} bytes) as {} -> {}",
                file.getOriginalFilename(), file.getSize(), storedFileName, url);

        return FileUploadResponse.builder()
                .url(url)
                .fileName(storedFileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    /** Stores multiple photos in one call, e.g. for multi-page document uploads. */
    public List<FileUploadResponse> storeAll(List<MultipartFile> files) {
        return files.stream().map(this::store).toList();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type '" + contentType + "'. Only image files (jpg, jpeg, png, webp, heic) are allowed.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file extension: ." + extension);
        }
    }

    private String extractExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            throw new IllegalArgumentException("Uploaded file must have a name with an extension.");
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
    }
}