package com.discipolat.common.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * P2 #83 — Isolation des fichiers par tenant
 * Les fichiers uploadés sont isolés dans /storage/{tenantId}/ pour éviter
 * les fuites de données entre églises (multi-tenancy).
 */
@Configuration
public class TenantFileIsolationConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // P2 #83 — Fichiers tenant isolés (pas d'accès direct aux fichiers d'un autre tenant)
        registry.addResourceHandler("/api/v1/files/{tenantId}/**")
                .addResourceLocations("file:./storage/")
                .setCachePeriod(3600);

        // P2 #83 — Assets publics (logo, favicon, etc.)
        registry.addResourceHandler("/public/**")
                .addResourceLocations("file:./storage/public/")
                .setCachePeriod(86400);
    }
}
