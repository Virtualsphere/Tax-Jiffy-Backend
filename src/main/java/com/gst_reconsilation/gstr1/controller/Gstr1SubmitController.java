package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr1.dto.Gstr1SubmitCredentials;
import com.gst_reconsilation.gstr1.dto.Gstr1SubmitResult;
import com.gst_reconsilation.gstr1.dto.api.WhitebooksGstr1Payload;
import com.gst_reconsilation.gstr1.service.Gstr1SubmitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "GSTR-1 Submit", description = "Preview and submit the final GSTR-1 payload to the GST system")
@RestController
@RequestMapping("/api/gstr1")
@RequiredArgsConstructor
public class Gstr1SubmitController {

    private final Gstr1SubmitService submitService;

    /**
     * GET /api/gstr1/filings/{filingId}/submit-payload
     * Builds and returns the exact JSON payload that would be sent to
     * PUT /gstr1/retsave, without actually submitting - for a final
     * "review before filing" confirmation screen. grossTurnover /
     * currentGrossTurnover aren't tracked anywhere else, so they're optional
     * query params here (default 0).
     */
    @GetMapping("/filings/{filingId}/submit-payload")
    public ResponseEntity<ApiResponse<WhitebooksGstr1Payload>> getSubmitPayload(
            @PathVariable Integer filingId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal grossTurnover,
            @RequestParam(required = false, defaultValue = "0") BigDecimal currentGrossTurnover) {

        WhitebooksGstr1Payload payload = submitService.buildSubmitPayload(filingId, grossTurnover, currentGrossTurnover);
        return ResponseEntity.ok(ApiResponse.success("OK", payload));
    }

    /**
     * POST /api/gstr1/filings/{filingId}/submit
     * Submits the filing to PUT /gstr1/retsave using the already-uploaded
     * GSTR-1 data. Call submit-payload first and get user confirmation before
     * calling this.
     */
    @PostMapping("/filings/{filingId}/submit")
    public ResponseEntity<ApiResponse<Gstr1SubmitResult>> submit(
            @PathVariable Integer filingId, @RequestBody Gstr1SubmitCredentials credentials, Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        Gstr1SubmitResult result = submitService.submit(filingId, credentials, userId);
        return ResponseEntity.ok(ApiResponse.success(result.isSuccess() ? "Filed successfully" : "Submission failed", result));
    }
}