package com.gst_reconsilation.admin.controller;

import com.gst_reconsilation.admin.dto.UserLastActiveResponse;
import com.gst_reconsilation.admin.service.AdminPresenceService;
import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.presence.dto.PresenceEvent;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Presence", description = "Super-admin: live user activity (REST snapshot; live updates are pushed over /topic/presence)")
@RestController
@RequestMapping("/api/admin/presence")
@RequiredArgsConstructor
public class AdminPresenceController {

    private final AdminPresenceService service;

    @GetMapping("/online")
    public ResponseEntity<ApiResponse<List<PresenceEvent>>> online() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listOnline()));
    }

    @GetMapping("/last-active")
    public ResponseEntity<ApiResponse<List<UserLastActiveResponse>>> lastActive() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listLastActive()));
    }
}
