package com.gst_reconsilation.company.controller;

import com.gst_reconsilation.company.dto.PurchaseSubscriptionRequest;
import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.company.dto.CompanyGSTRequest;
import com.gst_reconsilation.company.dto.CompanyGSTResponse;
import com.gst_reconsilation.company.service.CompanyGSTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Company GST", description = "Company GST registrations")
@RestController
@RequestMapping("/api/company-gst")
@RequiredArgsConstructor
public class CompanyGSTController {

    private final CompanyGSTService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyGSTResponse>> create(
            @RequestBody CompanyGSTRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyGSTResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @GetMapping("/by-company/{companyId}")
    public ResponseEntity<ApiResponse<List<CompanyGSTResponse>>> getByCompany(@PathVariable Integer companyId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByCompany(companyId)));
    }

    /** Active + deactivated GST numbers for this company — backs the Billing page's subscription history. */
    @GetMapping("/by-company/{companyId}/all")
    public ResponseEntity<ApiResponse<List<CompanyGSTResponse>>> getAllByCompany(@PathVariable Integer companyId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAllByCompany(companyId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyGSTResponse>> update(
            @PathVariable Integer id,
            @RequestBody CompanyGSTRequest req,
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

    @PostMapping("/{id}/purchase")
    public ResponseEntity<ApiResponse<CompanyGSTResponse>> purchase(
            @PathVariable Integer id,
            @RequestBody PurchaseSubscriptionRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Subscription activated",
                service.purchaseSubscription(id, req, userId)));
    }

    // NEW: upgrade an already-active subscription plan
    @PutMapping("/{id}/upgrade")
    public ResponseEntity<ApiResponse<CompanyGSTResponse>> upgrade(
            @PathVariable Integer id,
            @RequestBody PurchaseSubscriptionRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Subscription upgraded",
                service.upgradeSubscription(id, req, userId)));
    }
}