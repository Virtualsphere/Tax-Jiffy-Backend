package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr1.entity.Gstr1EwaybillReconciliationResult;
import com.gst_reconsilation.gstr1.service.Gstr1EwaybillReconciliationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "GSTR-1 vs E-Way Bill Reconciliation")
@RestController
@RequestMapping("/api/gstr1/filings/{filingId}/ewaybill-reco")
@RequiredArgsConstructor
public class Gstr1EwaybillReconciliationController {

    private final Gstr1EwaybillReconciliationService reconciliationService;

    /** Re-runs the bucket match against whatever e-way bill data already exists for this filing's
     *  GSTIN + period (synced or Excel-uploaded via the E-Way Bill page — no external API call here). */
    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<List<Gstr1EwaybillReconciliationResult>>> reconcile(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation complete", reconciliationService.reconcile(filingId)));
    }

    /** Full reconciliation result, optionally filtered by status. */
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<List<Gstr1EwaybillReconciliationResult>>> getResult(
            @PathVariable Integer filingId, @RequestParam(required = false) String matchStatus) {
        return ResponseEntity.ok(ApiResponse.success("OK", reconciliationService.getResult(filingId, matchStatus)));
    }

    /** Sale-register invoices with no e-way bill covering them yet. */
    @GetMapping("/unlinked")
    public ResponseEntity<ApiResponse<List<Gstr1EwaybillReconciliationResult>>> getUnlinked(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", reconciliationService.getUnlinked(filingId)));
    }
}
