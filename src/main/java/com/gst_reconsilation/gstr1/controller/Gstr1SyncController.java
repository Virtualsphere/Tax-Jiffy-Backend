package com.gst_reconsilation.gstr1.controller;

import com.gst_reconsilation.config.dto.ApiResponse;
import com.gst_reconsilation.gstr1.dto.SyncRequest;
import com.gst_reconsilation.gstr1.dto.SyncResponse;
import com.gst_reconsilation.gstr1.service.Gstr1SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "GSTR-1 Sync", description = "Sync GSTR-1 data from government API")
@RestController
@RequestMapping("/api/gstr1/sync")
@RequiredArgsConstructor
public class Gstr1SyncController {

    private final Gstr1SyncService service;
    @PostMapping
    public ResponseEntity<ApiResponse<SyncResponse>> sync(
            @RequestBody SyncRequest req,
            Authentication auth) {

        Integer userId = (Integer) auth.getPrincipal();
        SyncResponse result = service.sync(req, userId);
        return ResponseEntity.ok(ApiResponse.success("Sync complete", result));
    }
}