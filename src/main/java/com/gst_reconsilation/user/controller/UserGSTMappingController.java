package com.gst_reconsilation.user.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.user.dto.UserGSTMappingRequest;
import com.gst_reconsilation.user.dto.UserGSTMappingResponse;
import com.gst_reconsilation.user.service.UserGSTMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User-GST Mapping", description = "Map users to GST registrations with roles")
@RestController
@RequestMapping("/api/user-gst-mapping")
@RequiredArgsConstructor
public class UserGSTMappingController {

    private final UserGSTMappingService service;

    @PostMapping
    public ResponseEntity<ApiResponse<UserGSTMappingResponse>> create(
            @RequestBody UserGSTMappingRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ApiResponse<List<UserGSTMappingResponse>>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByUser(userId)));
    }

    @GetMapping("/by-company-gst/{companyGstId}")
    public ResponseEntity<ApiResponse<List<UserGSTMappingResponse>>> getByCompanyGST(@PathVariable Integer companyGstId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByCompanyGST(companyGstId)));
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