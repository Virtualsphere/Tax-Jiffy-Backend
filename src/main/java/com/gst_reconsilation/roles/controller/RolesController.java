package com.gst_reconsilation.roles.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.roles.dto.RolesRequest;
import com.gst_reconsilation.roles.dto.RolesResponse;
import com.gst_reconsilation.roles.service.RolesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Roles", description = "Role management")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolesController {

    private final RolesService service;

    @PostMapping
    public ResponseEntity<ApiResponse<RolesResponse>> create(
            @RequestBody RolesRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RolesResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RolesResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RolesResponse>> update(
            @PathVariable Integer id,
            @RequestBody RolesRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Updated", service.update(id, req, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Integer id,
            Authentication auth) {
        service.deactivate(id, (Integer) auth.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success("Deactivated", null));
    }
}
