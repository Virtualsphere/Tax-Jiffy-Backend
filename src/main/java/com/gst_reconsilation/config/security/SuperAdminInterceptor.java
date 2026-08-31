package com.gst_reconsilation.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gst_reconsilation.config.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Gates every /api/admin/** endpoint on isSuperAdmin, in one place, instead of an
 * inline check repeated in each new admin controller method. JwtAuthFilter already
 * runs before this (see SecurityConfig's filter chain) and puts the caller's userId
 * as the Authentication principal — this just adds the super-admin check on top of
 * the "authenticated" check /api/admin/** already gets from SecurityConfig.
 */
@Component
@RequiredArgsConstructor
public class SuperAdminInterceptor implements HandlerInterceptor {

    private final SuperAdminGuard superAdminGuard;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        Integer userId = principal instanceof Integer ? (Integer) principal : null;

        if (superAdminGuard.isSuperAdmin(userId)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error("Super admin access required")));
        return false;
    }
}
