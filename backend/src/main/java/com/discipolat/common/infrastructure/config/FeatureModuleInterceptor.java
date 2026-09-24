package com.discipolat.common.infrastructure.config;

import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FeatureModuleInterceptor implements HandlerInterceptor {

    private final PlatformFeatureFlagService featureFlagService;

    public FeatureModuleInterceptor(PlatformFeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isDocumentationRequest(request.getRequestURI())) {
            featureFlagService.requireEnabled(PlatformFeatureFlagService.DOCS_ENABLED);
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
