package com.gst_reconsilation.admin.controller;

import com.gst_reconsilation.admin.service.AdminSubscriptionService;
import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.subscription.dto.SubscriptionPurchaseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Subscription History", description = "Super-admin: platform-wide subscription purchase/upgrade history")
@RestController
@RequestMapping("/api/admin/subscription-purchases")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final AdminSubscriptionService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPurchaseResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listAll()));
    }

    @GetMapping("/by-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<SubscriptionPurchaseResponse>>> listByGst(@PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listByGst(companyGstId)));
    }
}
