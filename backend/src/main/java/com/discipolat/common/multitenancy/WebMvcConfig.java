package com.discipolat.common.multitenancy;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final TenantFilterInterceptor tenantFilterInterceptor;

    public WebMvcConfig(TenantInterceptor tenantInterceptor,
                        @Lazy TenantFilterInterceptor tenantFilterInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.tenantFilterInterceptor = tenantFilterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(tenantFilterInterceptor)
                .addPathPatterns("/api/**");
    }
}
