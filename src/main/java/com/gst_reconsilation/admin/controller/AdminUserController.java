package com.gst_reconsilation.admin.controller;

import com.gst_reconsilation.admin.dto.AdminUserSummaryResponse;
import com.gst_reconsilation.admin.service.AdminUserService;
import com.gst_reconsilation.config.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Users", description = "Super-admin: platform-wide user view")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserSummaryResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listAll()));
    }
}
