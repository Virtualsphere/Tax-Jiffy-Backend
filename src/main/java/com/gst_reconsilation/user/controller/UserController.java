package com.gst_reconsilation.user.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.user.dto.UserRequest;
import com.gst_reconsilation.user.dto.UserResponse;
import com.gst_reconsilation.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users", description = "User management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @RequestBody UserRequest req,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(req, userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getById(id)));
    }

    @GetMapping("/by-company/{companyId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getByCompany(@PathVariable Integer companyId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getByCompany(companyId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Integer id,
            @RequestBody UserRequest req,
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