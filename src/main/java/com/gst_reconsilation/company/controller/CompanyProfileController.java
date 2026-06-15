package com.gst_reconsilation.company.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.company.dto.CompanyProfileRequest;
import com.gst_reconsilation.company.dto.CompanyProfileResponse;
import com.gst_reconsilation.company.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Companies", description = "Company profile management")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> create(
            @RequestBody CompanyProfileRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyProfileResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> update(
            @PathVariable Integer id,
            @RequestBody CompanyProfileRequest req,
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