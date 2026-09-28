package com.discipolat.common.multitenancy;

import com.discipolat.common.infrastructure.config.FeatureModuleInterceptor;
import com.discipolat.common.infrastructure.observability.EndpointUsageInterceptor;
import com.discipolat.common.infrastructure.observability.EndpointUsageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final TenantFilterInterceptor tenantFilterInterceptor;
    private final TenantStatusInterceptor tenantStatusInterceptor;
    private final FeatureModuleInterceptor featureModuleInterceptor;
    private final EndpointUsageInterceptor endpointUsageInterceptor;

    public WebMvcConfig(TenantInterceptor tenantInterceptor,
                         @Lazy TenantFilterInterceptor tenantFilterInterceptor,
                         @Lazy TenantStatusInterceptor tenantStatusInterceptor,
                         ObjectProvider<FeatureModuleInterceptor> featureModuleInterceptorProvider,
                         ObjectProvider<EndpointUsageService> endpointUsageServiceProvider) {
        this.tenantInterceptor = tenantInterceptor;
        this.tenantFilterInterceptor = tenantFilterInterceptor;
        this.tenantStatusInterceptor = tenantStatusInterceptor;
        this.featureModuleInterceptor = featureModuleInterceptorProvider.getIfAvailable();
        // Optionnel : absent des contextes réduits (@WebMvcTest) → intercepteur désactivé.
        EndpointUsageService usageService = endpointUsageServiceProvider.getIfAvailable();
        this.endpointUsageInterceptor = usageService != null ? new EndpointUsageInterceptor(usageService) : null;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(tenantFilterInterceptor)
                .addPathPatterns("/api/**");
        // Constat B1 : la suspension d'un tenant doit être appliquée à TOUTE requête
        // authentifiée, pas seulement au login. Enregistré APRÈS le filtre Hibernate
        // (le TenantContext est alors posé) et AVANT le contrôle d'accès aux modules.
        registry.addInterceptor(tenantStatusInterceptor)
                .addPathPatterns("/api/**");
        if (featureModuleInterceptor != null) {
            registry.addInterceptor(featureModuleInterceptor)
                    .addPathPatterns("/api-docs/**", "/api-docs", "/swagger-ui/**", "/swagger-ui.html",
                            "/api/v1/api-docs/**", "/api/v1/public/docs/**");
        }
        // Mesure d'usage des endpoints (rapport de code mort plateforme)
        if (endpointUsageInterceptor != null) {
            registry.addInterceptor(endpointUsageInterceptor)
                    .addPathPatterns("/api/**");
        }
    }
}

