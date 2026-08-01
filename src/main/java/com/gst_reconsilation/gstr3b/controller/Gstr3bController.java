package com.gst_reconsilation.gstr3b.controller;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr3b.dto.*;
import com.gst_reconsilation.gstr3b.entity.Gstr3bFiling;
import com.gst_reconsilation.gstr3b.entity.Gstr3bReconciliationResult;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.repository.Gstr3bReconciliationResultRepository;
import com.gst_reconsilation.gstr3b.service.Gstr3bPreviewService;
import com.gst_reconsilation.gstr3b.service.Gstr3bReconciliationService;
import com.gst_reconsilation.gstr3b.service.Gstr3bTwoBSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "GSTR-3B", description = "GSTR-3B filing period management, GSTR-2B sync, IMS-vs-purchase-register reconciliation, and preview generation")
@RestController
@RequestMapping("/api/gstr3b")
@RequiredArgsConstructor
public class Gstr3bController {

    private final Gstr3bFilingRepository filingRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final Gstr3bTwoBSyncService twoBSyncService;
    private final Gstr3bPreviewService previewService;
    private final Gstr3bReconciliationService reconciliationService;
    private final Gstr3bReconciliationResultRepository reconciliationResultRepository;

    @PostMapping("/filings")
    public ResponseEntity<ApiResponse<Gstr3bFiling>> createOrLinkFiling(
            @RequestBody Gstr3bFilingLinkRequest req, Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        Gstr3bFiling filing = filingRepository
                .findByCompanyGST_IdAndFinancialYearAndTaxPeriod(req.getCompanyGstId(), req.getFinancialYear(), req.getTaxPeriod())
                .orElseGet(() -> Gstr3bFiling.builder()
                        .companyGST(companyGST)
                        .financialYear(req.getFinancialYear())
                        .taxPeriod(req.getTaxPeriod())
                        .createdBy(userId)
                        .build());

        if (req.getGstr1FilingId() != null) filing.setGstr1FilingId(req.getGstr1FilingId());
        if (req.getGstr2FilingId() != null) filing.setGstr2FilingId(req.getGstr2FilingId());
        if (req.getImsFilingId() != null) filing.setImsFilingId(req.getImsFilingId());
        filing.setUpdatedBy(userId);
        filing.setUpdatedDate(LocalDate.now());

        return ResponseEntity.ok(ApiResponse.success("OK", filingRepository.save(filing)));
    }

    @GetMapping("/filings/by-company-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<Gstr3bFiling>>> getFilingsByCompanyGST(@PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", filingRepository.findByCompanyGST_IdAndIsActiveTrue(companyGstId)));
    }

    @GetMapping("/filings/{filingId}")
    public ResponseEntity<ApiResponse<Gstr3bFiling>> getFilingById(@PathVariable Integer filingId) {
        Gstr3bFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + filingId));
        return ResponseEntity.ok(ApiResponse.success("OK", filing));
    }

    @PostMapping("/filings/{filingId}/sync-2b")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sync2b(
            @PathVariable Integer filingId, @RequestBody TwoBCredentials credentials, Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        int rows = twoBSyncService.sync(filingId, credentials, userId);
        return ResponseEntity.ok(ApiResponse.success("GSTR-2B sync complete", Map.of("rowsSynced", rows)));
    }

    @PutMapping("/filings/{filingId}/interest-late-fee")
    public ResponseEntity<ApiResponse<Gstr3bFiling>> updateInterestLateFee(
            @PathVariable Integer filingId, @RequestBody Gstr3bInterestLateFeeRequest req, Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        Gstr3bFiling filing = filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Gstr3bFiling not found: " + filingId));

        if (req.getInterestIntegratedTax() != null) filing.setInterestIntegratedTax(req.getInterestIntegratedTax());
        if (req.getInterestCentralTax() != null) filing.setInterestCentralTax(req.getInterestCentralTax());
        if (req.getInterestStateUtTax() != null) filing.setInterestStateUtTax(req.getInterestStateUtTax());
        if (req.getInterestCess() != null) filing.setInterestCess(req.getInterestCess());
        if (req.getLateFeeCentralTax() != null) filing.setLateFeeCentralTax(req.getLateFeeCentralTax());
        if (req.getLateFeeStateUtTax() != null) filing.setLateFeeStateUtTax(req.getLateFeeStateUtTax());
        filing.setUpdatedBy(userId);
        filing.setUpdatedDate(LocalDate.now());

        return ResponseEntity.ok(ApiResponse.success("OK", filingRepository.save(filing)));
    }

    /** Runs the purchase-register vs IMS comparison and persists the result. */
    @PostMapping("/filings/{filingId}/reconcile")
    public ResponseEntity<ApiResponse<List<Gstr3bReconciliationResult>>> reconcile(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation complete", reconciliationService.reconcile(filingId)));
    }

    /** Returns the last-computed reconciliation result without re-running it. */
    @GetMapping("/filings/{filingId}/reconciliation")
    public ResponseEntity<ApiResponse<List<Gstr3bReconciliationResult>>> getReconciliation(
            @PathVariable Integer filingId,
            @RequestParam(required = false) String matchStatus) {
        List<Gstr3bReconciliationResult> results = matchStatus != null
                ? reconciliationResultRepository.findByFiling_IdAndMatchStatus(filingId, matchStatus)
                : reconciliationResultRepository.findByFiling_Id(filingId);
        return ResponseEntity.ok(ApiResponse.success("OK", results));
    }

    @GetMapping("/filings/{filingId}/preview")
    public ResponseEntity<ApiResponse<Gstr3bPreviewResponse>> getPreview(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", previewService.buildPreview(filingId)));
    }
}