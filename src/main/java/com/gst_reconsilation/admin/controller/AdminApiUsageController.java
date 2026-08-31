package com.gst_reconsilation.admin.controller;

import com.gst_reconsilation.admin.dto.ApiUsageOverrideRequest;
import com.gst_reconsilation.admin.dto.ApiUsageSummaryResponse;
import com.gst_reconsilation.admin.service.AdminApiUsageService;
import com.gst_reconsilation.config.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - API Usage", description = "Super-admin: 3rd-party GST-portal API usage and per-GST limit overrides")
@RestController
@RequestMapping("/api/admin/api-usage")
@RequiredArgsConstructor
public class AdminApiUsageController {

    private final AdminApiUsageService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiUsageSummaryResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listAll()));
    }

    @GetMapping("/by-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<ApiUsageSummaryResponse>>> listByGst(@PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listByGst(companyGstId)));
    }

    @PutMapping("/by-gst/{companyGstId}/override")
    public ResponseEntity<ApiResponse<Void>> setOverride(
            @PathVariable Integer companyGstId, @RequestBody ApiUsageOverrideRequest req) {
        service.setOverride(companyGstId, req.getApiCallLimitOverride());
        return ResponseEntity.ok(ApiResponse.success("Updated", null));
    }
}
