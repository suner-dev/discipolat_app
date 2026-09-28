package com.discipolat.common.infrastructure.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Intercepteur de mesure d'usage : compte chaque appel d'endpoint en utilisant
 * le patron de route exact résolu par le HandlerMapping (ex.
 * {@code /api/v1/members/{id}}), jamais l'URL brute — ni identifiants, ni
 * explosion de cardinalité.
 *
 * <p>Coût par requête : deux lectures d'attributs + un increment atomique.</p>
 */
public class EndpointUsageInterceptor implements HandlerInterceptor {

    private final EndpointUsageService usageService;

    public EndpointUsageInterceptor(EndpointUsageService usageService) {
        this.usageService = usageService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return; // ressources statiques / 404 non mappés
        }
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = pattern instanceof String s ? s : request.getRequestURI();
        // Les endpoints de mesure eux-mêmes et l'actuator ne sont pas comptés
        if (route.startsWith("/actuator") || route.startsWith("/api/v1/platform/admin/usage")) {
            return;
        }
        usageService.record(request.getMethod(), route, response.getStatus() >= 500 || ex != null);
    }
}
