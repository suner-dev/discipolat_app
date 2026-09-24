package com.discipolat.common.infrastructure.config;

import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FeatureModuleInterceptor implements HandlerInterceptor {

    private final ObjectProvider<PlatformFeatureFlagService> featureFlagServiceProvider;

    public FeatureModuleInterceptor(ObjectProvider<PlatformFeatureFlagService> featureFlagServiceProvider) {
        this.featureFlagServiceProvider = featureFlagServiceProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isDocumentationRequest(request.getRequestURI())) {
            PlatformFeatureFlagService service = featureFlagServiceProvider.getIfAvailable();
            if (service == null) {
                throw new IllegalStateException("Documentation feature flag service unavailable");
            }
            service.requireEnabled(PlatformFeatureFlagService.DOCS_ENABLED);
        }
        return true;
    }

    private boolean isDocumentationRequest(String path) {
        if (path == null) {
            return false;
        }
        return path.equals("/api-docs")
                || path.startsWith("/api-docs/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/api/v1/api-docs")
                || path.startsWith("/api/v1/api-docs/")
                || path.equals("/api/v1/public/docs")
                || path.startsWith("/api/v1/public/docs/");
    }
}
