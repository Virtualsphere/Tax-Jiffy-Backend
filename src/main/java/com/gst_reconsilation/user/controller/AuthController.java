package com.gst_reconsilation.user.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.user.dto.LoginRequest;
import com.gst_reconsilation.user.dto.LoginResponse;
import com.gst_reconsilation.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "Login and authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authService.login(req)));
    }
}
