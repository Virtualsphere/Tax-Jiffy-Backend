package com.gst_reconsilation.whitebooks.session;

import com.gst_reconsilation.config.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "GST Portal Session", description = "OTP-based GSTN session (txn) used by all GST API syncs")
@RestController
@RequestMapping("/api/gst-session/{companyGstId}")
@RequiredArgsConstructor
public class GstSessionController {

    private final GstAuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<GstSessionDtos.SessionStatus>> status(@PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", authService.status(companyGstId)));
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponse<GstSessionDtos.SessionStatus>> requestOtp(
            @PathVariable Integer companyGstId, @RequestBody GstSessionDtos.OtpRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("OTP sent to the taxpayer's registered mobile/email",
                authService.requestOtp(companyGstId, req.getGstUsername(), userId)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<GstSessionDtos.SessionStatus>> verifyOtp(
            @PathVariable Integer companyGstId, @RequestBody GstSessionDtos.OtpVerifyRequest req, Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("GST portal session active",
                authService.verifyOtp(companyGstId, req.getOtp(), userId)));
    }
}
