// ewaybill/controller/EwaybillController.java
package com.gst_reconsilation.ewaybill.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.ewaybill.dto.*;
import com.gst_reconsilation.ewaybill.entity.EwaybillFiling;
import com.gst_reconsilation.ewaybill.entity.EwaybillRecord;
import com.gst_reconsilation.ewaybill.service.EwaybillSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "E-Way Bill", description = "Sync and manage e-way bill data")
@RestController
@RequestMapping("/api/ewaybill")
@RequiredArgsConstructor
public class EwaybillController {

    private final EwaybillSyncService service;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<EwaybillFiling>> syncByDate(
            @RequestBody EwaybillSyncByDateRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Sync complete", service.syncByDate(req, userId)));
    }

    @PostMapping("/sync/{ewbNo}")
    public ResponseEntity<ApiResponse<EwaybillRecord>> syncSingle(
            @PathVariable Long ewbNo, @RequestParam Integer companyGstId, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Sync complete", service.syncSingle(companyGstId, ewbNo, userId)));
    }

    @PostMapping("/records")
    public ResponseEntity<ApiResponse<EwaybillRecord>> createManual(
            @RequestBody EwaybillManualRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.createManual(req, userId)));
    }

    @GetMapping("/filings/{filingId}/records")
    public ResponseEntity<ApiResponse<List<EwaybillRecord>>> getByFiling(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByFiling(filingId)));
    }

    // EwaybillController.java — add
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EwaybillFiling>> uploadExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam Integer companyGstId,
            @RequestParam String syncDate,
            Authentication auth) throws Exception {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Uploaded", service.uploadExcel(file, companyGstId, syncDate, userId)));
    }
}