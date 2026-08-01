// gstr1/controller/Gstr1EinvoiceReconciliationController.java
package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.einvoice.entity.EinvoiceIrn;
import com.gst_reconsilation.gstr1.entity.Gstr1B2b;
import com.gst_reconsilation.gstr1.entity.Gstr1EinvoiceReconciliationResult;
import com.gst_reconsilation.gstr1.entity.Gstr1Filing;
import com.gst_reconsilation.gstr1.repository.Gstr1FilingRepository;
import com.gst_reconsilation.gstr1.service.Gstr1EinvoiceReconciliationService;
import com.gst_reconsilation.gstr1.service.Gstr1UploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "GSTR-1 vs E-Invoice Reconciliation")
@RestController
@RequestMapping("/api/gstr1/filings/{filingId}/einvoice-reco")
@RequiredArgsConstructor
public class Gstr1EinvoiceReconciliationController {

    private final Gstr1EinvoiceReconciliationService reconciliationService;
    private final Gstr1UploadService gstr1UploadService;
    private final Gstr1FilingRepository filingRepository;

    /** Sync e-invoice data for this filing's derived period + run reconciliation. */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sync(
            @PathVariable Integer filingId,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Sync + reconciliation complete",
                reconciliationService.syncAndReconcile(filingId, userId, force)));
    }

    /** Sale-register rows — each row's isPaired flag is what the frontend highlights on. */
    @GetMapping("/sale-register")
    public ResponseEntity<ApiResponse<List<Gstr1B2b>>> getSaleRegister(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", gstr1UploadService.getB2b(filingId)));
    }

    /** E-invoice rows for this filing's derived period, each carrying isPaired. */
    @GetMapping("/einvoices")
    public ResponseEntity<ApiResponse<List<EinvoiceIrn>>> getEinvoices(@PathVariable Integer filingId) {
        Gstr1Filing filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr1Filing not found: " + filingId));
        return ResponseEntity.ok(ApiResponse.success("OK", reconciliationService.getEinvoicesForFiling(filing)));
    }

    /** Full reconciliation result, optionally filtered by status. */
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<List<Gstr1EinvoiceReconciliationResult>>> getResult(
            @PathVariable Integer filingId, @RequestParam(required = false) String matchStatus) {
        return ResponseEntity.ok(ApiResponse.success("OK", reconciliationService.getResult(filingId, matchStatus)));
    }

    /** "Link" button — invoices booked in the sale register with no e-invoice yet. */
    @GetMapping("/unlinked")
    public ResponseEntity<ApiResponse<List<Gstr1EinvoiceReconciliationResult>>> getUnlinked(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", reconciliationService.getUnlinked(filingId)));
    }
}