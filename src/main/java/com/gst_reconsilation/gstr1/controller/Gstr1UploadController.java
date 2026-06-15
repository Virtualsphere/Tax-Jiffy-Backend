package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr1.dto.Gstr1UploadResponse;
import com.gst_reconsilation.gstr1.entity.*;
import com.gst_reconsilation.gstr1.service.Gstr1UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "GSTR-1 Upload", description = "Upload GSTR-1 Excel files and retrieve parsed data")
@RestController
@RequestMapping("/api/gstr1")
@RequiredArgsConstructor
public class Gstr1UploadController {

    private final Gstr1UploadService service;

    /**
     * Upload a GSTR-1 Excel file.
     * POST /api/gstr1/upload
     * Content-Type: multipart/form-data
     * Form params: file, companyGstId, financialYear, taxPeriod
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Gstr1UploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("companyGstId") Integer companyGstId,
            @RequestParam("financialYear") String financialYear,
            @RequestParam("taxPeriod") String taxPeriod,
            Authentication auth) throws Exception {

        Integer userId = (Integer) auth.getPrincipal();
        Gstr1UploadResponse response = service.uploadAndProcess(
                file, companyGstId, financialYear, taxPeriod, userId);
        return ResponseEntity.ok(ApiResponse.success("Uploaded and processed", response));
    }

    // ── Filing queries ────────────────────────────────────────────

    @GetMapping("/filings/by-company-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<Gstr1Filing>>> getFilingsByCompanyGST(
            @PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                service.getFilingsByCompanyGST(companyGstId)));
    }

    @GetMapping("/filings/{filingId}")
    public ResponseEntity<ApiResponse<Gstr1Filing>> getFilingById(
            @PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                service.getFilingById(filingId)));
    }

    // ── Per-sheet data endpoints ──────────────────────────────────

    @GetMapping("/filings/{filingId}/b2b")
    public ResponseEntity<ApiResponse<List<Gstr1B2b>>> getB2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/b2ba")
    public ResponseEntity<ApiResponse<List<Gstr1B2ba>>> getB2ba(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2ba(filingId)));
    }

    @GetMapping("/filings/{filingId}/b2cl")
    public ResponseEntity<ApiResponse<List<Gstr1B2cl>>> getB2cl(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2cl(filingId)));
    }

    @GetMapping("/filings/{filingId}/b2cla")
    public ResponseEntity<ApiResponse<List<Gstr1B2cla>>> getB2cla(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2cla(filingId)));
    }

    @GetMapping("/filings/{filingId}/b2cs")
    public ResponseEntity<ApiResponse<List<Gstr1B2cs>>> getB2cs(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2cs(filingId)));
    }

    @GetMapping("/filings/{filingId}/b2csa")
    public ResponseEntity<ApiResponse<List<Gstr1B2csa>>> getB2csa(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getB2csa(filingId)));
    }

    @GetMapping("/filings/{filingId}/cdnr")
    public ResponseEntity<ApiResponse<List<Gstr1Cdnr>>> getCdnr(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCdnr(filingId)));
    }

    @GetMapping("/filings/{filingId}/cdnra")
    public ResponseEntity<ApiResponse<List<Gstr1Cdnra>>> getCdnra(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCdnra(filingId)));
    }

    @GetMapping("/filings/{filingId}/cdnur")
    public ResponseEntity<ApiResponse<List<Gstr1Cdnur>>> getCdnur(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCdnur(filingId)));
    }

    @GetMapping("/filings/{filingId}/cdnura")
    public ResponseEntity<ApiResponse<List<Gstr1Cdnura>>> getCdnura(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCdnura(filingId)));
    }

    @GetMapping("/filings/{filingId}/exp")
    public ResponseEntity<ApiResponse<List<Gstr1Exp>>> getExp(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getExp(filingId)));
    }

    @GetMapping("/filings/{filingId}/expa")
    public ResponseEntity<ApiResponse<List<Gstr1Expa>>> getExpa(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getExpa(filingId)));
    }

    @GetMapping("/filings/{filingId}/at")
    public ResponseEntity<ApiResponse<List<Gstr1At>>> getAt(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAt(filingId)));
    }

    @GetMapping("/filings/{filingId}/ata")
    public ResponseEntity<ApiResponse<List<Gstr1Ata>>> getAta(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAta(filingId)));
    }

    @GetMapping("/filings/{filingId}/atadj")
    public ResponseEntity<ApiResponse<List<Gstr1Atadj>>> getAtadj(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAtadj(filingId)));
    }

    @GetMapping("/filings/{filingId}/atadja")
    public ResponseEntity<ApiResponse<List<Gstr1Atadja>>> getAtadja(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAtadja(filingId)));
    }

    @GetMapping("/filings/{filingId}/exemp")
    public ResponseEntity<ApiResponse<List<Gstr1Exemp>>> getExemp(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getExemp(filingId)));
    }

    @GetMapping("/filings/{filingId}/hsn-b2b")
    public ResponseEntity<ApiResponse<List<Gstr1HsnB2b>>> getHsnB2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getHsnB2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/hsn-b2c")
    public ResponseEntity<ApiResponse<List<Gstr1HsnB2c>>> getHsnB2c(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getHsnB2c(filingId)));
    }

    @GetMapping("/filings/{filingId}/docs")
    public ResponseEntity<ApiResponse<List<Gstr1Docs>>> getDocs(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getDocs(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco")
    public ResponseEntity<ApiResponse<List<Gstr1Eco>>> getEco(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEco(filingId)));
    }

    @GetMapping("/filings/{filingId}/ecoa")
    public ResponseEntity<ApiResponse<List<Gstr1Ecoa>>> getEcoa(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoa(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-b2b")
    public ResponseEntity<ApiResponse<List<Gstr1EcoB2b>>> getEcoB2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoB2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-urp2b")
    public ResponseEntity<ApiResponse<List<Gstr1EcoUrp2b>>> getEcoUrp2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoUrp2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-b2c")
    public ResponseEntity<ApiResponse<List<Gstr1EcoB2c>>> getEcoB2c(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoB2c(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-urp2c")
    public ResponseEntity<ApiResponse<List<Gstr1EcoUrp2c>>> getEcoUrp2c(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoUrp2c(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-ab2b")
    public ResponseEntity<ApiResponse<List<Gstr1EcoAB2b>>> getEcoAB2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoAB2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-ab2c")
    public ResponseEntity<ApiResponse<List<Gstr1EcoAB2c>>> getEcoAB2c(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoAB2c(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-aurp2b")
    public ResponseEntity<ApiResponse<List<Gstr1EcoAUrp2b>>> getEcoAUrp2b(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoAUrp2b(filingId)));
    }

    @GetMapping("/filings/{filingId}/eco-aurp2c")
    public ResponseEntity<ApiResponse<List<Gstr1EcoAUrp2c>>> getEcoAUrp2c(@PathVariable Integer filingId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getEcoAUrp2c(filingId)));
    }
}