// ims/controller/ImsController.java
package com.gst_reconsilation.ims.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.ims.dto.*;
import com.gst_reconsilation.ims.entity.ImsInvoice;
import com.gst_reconsilation.ims.service.ImsSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Tag(name = "IMS", description = "Sync, upload, and manually manage IMS (Invoice Management System) data")
@RestController
@RequestMapping("/api/ims")
@RequiredArgsConstructor
public class ImsController {

    private final ImsSyncService service;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<ImsSyncResponse>> sync(@RequestBody ImsSyncRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Sync complete", service.sync(req, userId)));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImsUploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam Integer companyGstId,
            @RequestParam String retPeriod,
            Authentication auth) throws Exception {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Uploaded", service.uploadExcel(file, companyGstId, retPeriod, userId)));
    }

    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<ImsInvoice>> createManual(@RequestBody ImsManualRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.createManual(req, userId)));
    }

    @GetMapping("/filings/{filingId}/invoices")
    public ResponseEntity<ApiResponse<List<ImsInvoice>>> getByFiling(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByFiling(filingId)));
    }
}