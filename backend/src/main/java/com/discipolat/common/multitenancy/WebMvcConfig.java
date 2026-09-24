package com.discipolat.common.multitenancy;

import com.discipolat.common.infrastructure.config.FeatureModuleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final TenantFilterInterceptor tenantFilterInterceptor;
    private final FeatureModuleInterceptor featureModuleInterceptor;

    public WebMvcConfig(TenantInterceptor tenantInterceptor,
                         @Lazy TenantFilterInterceptor tenantFilterInterceptor,
                         FeatureModuleInterceptor featureModuleInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.tenantFilterInterceptor = tenantFilterInterceptor;
        this.featureModuleInterceptor = featureModuleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(tenantFilterInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(featureModuleInterceptor)
                .addPathPatterns("/api-docs/**", "/api-docs", "/swagger-ui/**", "/swagger-ui.html",
                        "/api/v1/api-docs/**", "/api/v1/public/docs/**");
    }
}

