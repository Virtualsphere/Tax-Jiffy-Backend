// config/SwaggerConfig.java
package com.gst_reconsilation.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GST Reconciliation System API")
                        .description("APIs for user management, company GST, GSTR-1 upload and sync")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GST Reconciliation")
                                .email("support@gstreconciliation.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                                .name("Bearer Auth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here. Get it from POST /api/auth/login")));
    }
}