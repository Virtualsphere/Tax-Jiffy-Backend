package com.gst_reconsilation.rolesmapping.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.rolesmapping.dto.RoleMappingRequest;
import com.gst_reconsilation.rolesmapping.dto.RoleMappingResponse;
import com.gst_reconsilation.rolesmapping.service.RoleMappingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Role Mapping", description = "Page and screen level permissions per role and GST")
@RestController
@RequestMapping("/api/role-mapping")
@RequiredArgsConstructor
public class RoleMappingController {

    private final RoleMappingService service;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleMappingResponse>> create(
            @RequestBody RoleMappingRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleMappingResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @GetMapping("/by-company-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<RoleMappingResponse>>> getByCompanyGST(
            @PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByCompanyGST(companyGstId)));
    }

    @GetMapping("/by-role-and-gst")
    public ResponseEntity<ApiResponse<List<RoleMappingResponse>>> getByRoleAndGST(
            @RequestParam Integer roleId,
            @RequestParam Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByRoleAndGST(roleId, companyGstId)));
    }

    @GetMapping("/by-company/{companyId}")
    public ResponseEntity<ApiResponse<List<RoleMappingResponse>>> getByCompany(
            @PathVariable Integer companyId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByCompany(companyId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleMappingResponse>> update(
            @PathVariable Integer id,
            @RequestBody RoleMappingRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Updated", service.update(id, req, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer id,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        service.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}