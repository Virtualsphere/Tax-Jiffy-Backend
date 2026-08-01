// einvoice/controller/EinvoiceController.java
package com.gst_reconsilation.einvoice.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.einvoice.dto.*;
import com.gst_reconsilation.einvoice.entity.EinvoiceHsnSummary;
import com.gst_reconsilation.einvoice.entity.EinvoiceIrn;
import com.gst_reconsilation.einvoice.service.EinvoiceSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "E-Invoice", description = "Sync and manage e-invoice (IRN) data")
@RestController
@RequestMapping("/api/einvoice")
@RequiredArgsConstructor
public class EinvoiceController {

    private final EinvoiceSyncService service;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<EinvoiceSyncResponse>> sync(
            @RequestBody EinvoiceSyncRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Sync complete", service.sync(req, userId)));
    }

    @PostMapping("/irns/{irn}/detail")
    public ResponseEntity<ApiResponse<EinvoiceIrn>> fetchDetail(@PathVariable String irn) {
        return ResponseEntity.ok(ApiResponse.success("Fetched", service.fetchAndStoreIrnDetail(irn)));
    }

    @PostMapping("/irns")
    public ResponseEntity<ApiResponse<EinvoiceIrn>> createManual(
            @RequestBody EinvoiceIrnManualRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.createManual(req, userId)));
    }

    @GetMapping("/filings/{filingId}/irns")
    public ResponseEntity<ApiResponse<List<EinvoiceIrn>>> getIrns(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getIrnsByFiling(filingId)));
    }

    @GetMapping("/filings/{filingId}/hsn")
    public ResponseEntity<ApiResponse<List<EinvoiceHsnSummary>>> getHsn(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getHsnByFiling(filingId)));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EinvoiceSyncResponse>> uploadExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam Integer companyGstId,
            @RequestParam String retPeriod,
            Authentication auth) throws Exception {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Uploaded", service.uploadExcel(file, companyGstId, retPeriod, userId)));
    }
}