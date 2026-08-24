package com.discipolat.common.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Discipolat API")
                        .version("1.0.0")
                        .description("API complète pour la gestion d'églises — Discipolat. " +
                                "Authentification JWT RS256, multi-tenant, RBAC. " +
                                "Tous les endpoints (sauf /auth/**) nécessitent un token Bearer.")
                        .contact(new Contact()
                                .name("Discipolat Support")
                                .email("support@discipolat.com")
                                .url("https://discipolat.com"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://discipolat.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .schemaRequirement("Bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT RS256 token. " +
                                "Récupérez-le via POST /api/v1/auth/login " +
                                "puis utilisez-le comme: Authorization: Bearer <token>"));
    }
}
