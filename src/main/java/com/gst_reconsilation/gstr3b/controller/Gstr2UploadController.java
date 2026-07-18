package com.gst_reconsilation.gstr3b.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr3b.dto.Gstr2UploadResponse;
import com.gst_reconsilation.gstr3b.entity.*;
import com.gst_reconsilation.gstr3b.service.Gstr2UploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "GSTR-2 Upload", description = "Upload GSTR-2 (purchase return) Excel files and retrieve parsed data")
@RestController
@RequestMapping("/api/gstr2")
@RequiredArgsConstructor
public class Gstr2UploadController {

    private final Gstr2UploadService service;

    /**
     * Upload a GSTR-2 Excel file.
     * POST /api/gstr2/upload
     * Content-Type: multipart/form-data
     * Form params: file, companyGstId, financialYear, taxPeriod
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Gstr2UploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("companyGstId") Integer companyGstId,
            @RequestParam("financialYear") String financialYear,
            @RequestParam("taxPeriod") String taxPeriod,
            Authentication auth) throws Exception {

        Integer userId = (Integer) auth.getPrincipal();
        Gstr2UploadResponse response = service.uploadAndProcess(
                file, companyGstId, financialYear, taxPeriod, userId);
        return ResponseEntity.ok(ApiResponse.success("Uploaded and processed", response));
    }

    // ── Filing queries ────────────────────────────────────────────

    @GetMapping("/filings/by-company-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<Gstr2Filing>>> getFilingsByCompanyGST(
            @PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                service.getFilingsByCompanyGST(companyGstId)));
    }

    @GetMapping("/filings/{filingId}")
    public ResponseEntity<ApiResponse<Gstr2Filing>> getFilingById(
            @PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                service.getFilingById(filingId)));
    }

    // ── Per-sheet data endpoints ──────────────────────────────────

    @GetMapping("/filings/{filingId}/b2b")
    public ResponseEntity<ApiResponse<List<Gstr2B2b>>> getB2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/b2bur")
    public ResponseEntity<ApiResponse<List<Gstr2B2bur>>> getB2bur(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2bur(filingId)));
    }

    @GetMapping("/filings/{filingId}/imps")
    public ResponseEntity<ApiResponse<List<Gstr2Imps>>> getImps(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getImps(filingId)));
    }

    @GetMapping("/filings/{filingId}/impg")
    public ResponseEntity<ApiResponse<List<Gstr2Impg>>> getImpg(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getImpg(filingId)));
    }

    @GetMapping("/filings/{filingId}/cdnr")
    public ResponseEntity<ApiResponse<List<Gstr2Cdnr>>> getCdnr(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCdnr(filingId)));
    }

    @GetMapping("/filings/{filingId}/cdnur")
    public ResponseEntity<ApiResponse<List<Gstr2Cdnur>>> getCdnur(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCdnur(filingId)));
    }

    @GetMapping("/filings/{filingId}/at")
    public ResponseEntity<ApiResponse<List<Gstr2At>>> getAt(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAt(filingId)));
    }

    @GetMapping("/filings/{filingId}/atadj")
    public ResponseEntity<ApiResponse<List<Gstr2Atadj>>> getAtadj(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAtadj(filingId)));
    }

    @GetMapping("/filings/{filingId}/exemp")
    public ResponseEntity<ApiResponse<List<Gstr2Exemp>>> getExemp(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getExemp(filingId)));
    }

    @GetMapping("/filings/{filingId}/itcr")
    public ResponseEntity<ApiResponse<List<Gstr2Itcr>>> getItcr(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getItcr(filingId)));
    }

    @GetMapping("/filings/{filingId}/hsn-sum")
    public ResponseEntity<ApiResponse<List<Gstr2HsnSum>>> getHsnSum(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getHsnSum(filingId)));
    }
}