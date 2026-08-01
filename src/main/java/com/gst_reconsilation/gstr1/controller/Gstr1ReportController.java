package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr1.dto.report.Gstr1ReportResponse;
import com.gst_reconsilation.gstr1.service.Gstr1PdfService;
import com.gst_reconsilation.gstr1.service.Gstr1ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final Gstr1PdfService pdfService;

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

    @GetMapping("/filings/{filingId}/report/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Integer filingId) {
        byte[] pdf = pdfService.generatePdf(filingId);

        String filename = "GSTR1_Filing_" + filingId + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}