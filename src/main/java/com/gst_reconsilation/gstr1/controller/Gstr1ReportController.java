package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr1.dto.report.Gstr1ReportResponse;
import com.gst_reconsilation.gstr1.service.Gstr1ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GSTR-1 Report", description = "Fetch the full sales-return (GSTR-1) style report for a filing")
@RestController
@RequestMapping("/api/gstr1")
@RequiredArgsConstructor
public class Gstr1ReportController {

    private final Gstr1ReportService reportService;

    /**
     * GET /api/gstr1/filings/{filingId}/report
     * Returns the full basicData / outwardData / amendmentsData / advancedData / othersData
     * report assembled from every sheet table linked to the given filing.
     */
    @GetMapping("/filings/{filingId}/report")
    public ResponseEntity<ApiResponse<Gstr1ReportResponse>> getReport(@PathVariable Integer filingId) {
        Gstr1ReportResponse report = reportService.buildReport(filingId);
        return ResponseEntity.ok(ApiResponse.success("OK", report));
    }
}