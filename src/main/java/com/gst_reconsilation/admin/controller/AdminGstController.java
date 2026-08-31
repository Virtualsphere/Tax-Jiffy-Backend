package com.gst_reconsilation.admin.controller;

import com.gst_reconsilation.admin.dto.AdminGstSummaryResponse;
import com.gst_reconsilation.admin.service.AdminGstService;
import com.gst_reconsilation.config.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - GST", description = "Super-admin: platform-wide GST number view")
@RestController
@RequestMapping("/api/admin/gst")
@RequiredArgsConstructor
public class AdminGstController {

    private final AdminGstService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminGstSummaryResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminGstSummaryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }
}
