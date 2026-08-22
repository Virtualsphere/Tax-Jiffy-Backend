// gstr2b/controller/Gstr2bController.java
package com.gst_reconsilation.gstr2b.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr2b.dto.Gstr2bInvoiceUpdateRequest;
import com.gst_reconsilation.gstr2b.dto.Gstr2bReconciliationRow;
import com.gst_reconsilation.gstr2b.service.Gstr2bReconciliationService;
import com.gst_reconsilation.gstr3b.entity.Gstr2B2b;
import com.gst_reconsilation.gstr3b.entity.Gstr2Filing;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GSTR-2B reconciliation against IMS. There is no separate GSTR-2B upload here — the
 * "uploaded GSTR-2B data" this page reconciles IS the purchase-register data
 * (Gstr2Filing / Gstr2B2b, uploaded via POST /api/gstr2/upload on the Purchase Register
 * page or from the GSTR-2B page pointed at the same endpoint). This controller only adds
 * the reconciliation, correction, and finalize actions on top of that existing data.
 */
@Tag(name = "GSTR-2B Reconciliation", description = "Reconcile uploaded GSTR-2B (purchase register) data against IMS for the same return period")
@RestController
@RequestMapping("/api/gstr2b")
@RequiredArgsConstructor
public class Gstr2bController {

    private final Gstr2bReconciliationService reconciliationService;

    @GetMapping("/filings/{gstr2FilingId}/reconciliation")
    public ResponseEntity<ApiResponse<List<Gstr2bReconciliationRow>>> getReconciliation(@PathVariable Integer gstr2FilingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", reconciliationService.reconcile(gstr2FilingId)));
    }

    @PatchMapping("/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<Gstr2B2b>> updateInvoice(
            @PathVariable Integer invoiceId, @RequestBody Gstr2bInvoiceUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Updated", reconciliationService.updateInvoice(invoiceId, req)));
    }

    @PostMapping("/filings/{gstr2FilingId}/finalize")
    public ResponseEntity<ApiResponse<Gstr2Filing>> finalize(@PathVariable Integer gstr2FilingId) {
        return ResponseEntity.ok(ApiResponse.success("Finalized", reconciliationService.finalizeFiling(gstr2FilingId)));
    }
}
