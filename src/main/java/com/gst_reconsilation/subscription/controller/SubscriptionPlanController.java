package com.gst_reconsilation.subscription.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.subscription.dto.SubscriptionPlanRequest;
import com.gst_reconsilation.subscription.dto.SubscriptionPlanResponse;
import com.gst_reconsilation.subscription.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Subscription Plans", description = "Manage subscription plans")
@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService service;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> create(
            @RequestBody SubscriptionPlanRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> update(
            @PathVariable Integer id,
            @RequestBody SubscriptionPlanRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Updated", service.update(id, req, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Integer id,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        service.deactivate(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Deactivated", null));
    }
}