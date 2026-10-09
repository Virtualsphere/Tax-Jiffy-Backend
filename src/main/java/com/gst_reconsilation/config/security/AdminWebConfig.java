package com.gst_reconsilation.config.security;

import com.gst_reconsilation.permission.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AdminWebConfig implements WebMvcConfigurer {

    private final SuperAdminInterceptor superAdminInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(superAdminInterceptor).addPathPatterns("/api/admin/**");
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**");
    }
}
