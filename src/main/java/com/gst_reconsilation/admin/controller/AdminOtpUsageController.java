package com.gst_reconsilation.admin.controller;

import com.gst_reconsilation.admin.dto.OtpUsageDtos;
import com.gst_reconsilation.admin.service.AdminOtpUsageService;
import com.gst_reconsilation.config.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - OTP Usage", description = "Super-admin: GST portal OTP requests/verifications per GSTIN")
@RestController
@RequestMapping("/api/admin/otp-usage")
@RequiredArgsConstructor
public class AdminOtpUsageController {

    private final AdminOtpUsageService service;

    /** @param period yyyy-MM; defaults to the current month */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OtpUsageDtos.Summary>>> summary(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.summary(period)));
    }

    @GetMapping("/by-gst/{companyGstId}/events")
    public ResponseEntity<ApiResponse<List<OtpUsageDtos.Event>>> events(
            @PathVariable Integer companyGstId, @RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.events(companyGstId, period)));
    }
}
