package com.gst_reconsilation.gstr3b.controller;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr3b.dto.*;
import com.gst_reconsilation.gstr3b.entity.Gstr3bFiling;
import com.gst_reconsilation.gstr3b.repository.Gstr3bFilingRepository;
import com.gst_reconsilation.gstr3b.service.Gstr3bImsSyncService;
import com.gst_reconsilation.gstr3b.service.Gstr3bPreviewService;
import com.gst_reconsilation.gstr3b.service.Gstr3bTwoBSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "GSTR-3B", description = "GSTR-3B filing period management, IMS/GSTR-2B sync for ITC reconciliation, and preview generation")
@RestController
@RequestMapping("/api/gstr3b")
@RequiredArgsConstructor
public class Gstr3bController {

    private final Gstr3bFilingRepository filingRepository;
    private final CompanyGSTRepository companyGSTRepository;
    private final Gstr3bImsSyncService imsSyncService;
    private final Gstr3bTwoBSyncService twoBSyncService;
    private final Gstr3bPreviewService previewService;

    /**
     * Creates (or fetches, if already present for the period) the Gstr3bFiling
     * anchor record and links it to the outward (GSTR-1) and inward (purchase
     * register) filings for the same period.
     */
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

    /**
     * Syncs supplier invoices from IMS (GET /ims/supplierinvoices) and stores
     * them as flattened, accept/reject/pending-tagged rows for this filing.
     */
    @PostMapping("/filings/{filingId}/sync-ims")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> syncIms(
            @PathVariable Integer filingId, @RequestBody ImsCredentials credentials, Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        int rows = imsSyncService.sync(filingId, credentials, userId);
        return ResponseEntity.ok(ApiResponse.success("IMS sync complete", Map.of("rowsSynced", rows)));
    }

    /**
     * Syncs the ITC summary from GSTR-2B (GET /gstr2b/all) for use in table 4
     * of the preview.
     */
    @PostMapping("/filings/{filingId}/sync-2b")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> sync2b(
            @PathVariable Integer filingId, @RequestBody TwoBCredentials credentials, Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        int rows = twoBSyncService.sync(filingId, credentials, userId);
        return ResponseEntity.ok(ApiResponse.success("GSTR-2B sync complete", Map.of("rowsSynced", rows)));
    }

    /** Manual entry for table 5.1 (Interest and Late Fee). */
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

    /**
     * Builds and returns the GSTR-3B preview (tables 3.1, 3.2, 4, 5, 5.1, 6.1)
     * for the frontend, reconciling outward (GSTR-1) + inward (purchase
     * register) + IMS/GSTR-2B synced data. Does not generate a challan or
     * submit anything to the GST system.
     */
    @GetMapping("/filings/{filingId}/preview")
    public ResponseEntity<ApiResponse<Gstr3bPreviewResponse>> getPreview(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", previewService.buildPreview(filingId)));
    }
}