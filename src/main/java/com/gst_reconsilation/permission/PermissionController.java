package com.gst_reconsilation.permission;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.permission.dto.MyPermissionsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Permissions", description = "The caller's effective page/screen permissions on a GST")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPermissionsResponse>> me(
            @RequestParam Integer companyGstId,
            Authentication auth) {
        Integer userId = (Integer) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("OK", service.getMyPermissions(userId, companyGstId)));
    }
}
