package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.observability.EndpointUsageDailyRepository;
import com.discipolat.common.infrastructure.observability.EndpointUsageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rapport plateforme d'usage des endpoints HTTP : quels routes sont appelées,
 * lesquelles jamais (candidats à la retraite) — la base factuelle pour
 * réduire la surface applicative avant de supprimer quoi que ce soit.
 */
@RestController
@RequestMapping("/api/v1/platform/admin/usage")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class EndpointUsageController {

    private final EndpointUsageService usageService;
    private final EndpointUsageDailyRepository usageRepository;
    private final RequestMappingHandlerMapping handlerMapping;

    public EndpointUsageController(EndpointUsageService usageService,
                                   EndpointUsageDailyRepository usageRepository,
                                   @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.usageService = usageService;
        this.usageRepository = usageRepository;
        this.handlerMapping = handlerMapping;
    }

    /**
     * Inventaire complet : chaque route mappée du backend, son volume d'appels
     * sur la période, et le drapeau {@code unused} quand aucun appel n'a été
     * observé depuis {@code days} jours (ni en base, ni en mémoire).
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> report(@RequestParam(defaultValue = "30") int days) {
        int window = Math.min(Math.max(days, 1), 365);
        LocalDate from = LocalDate.now().minusDays(window);

        Map<String, Long> usageTotals = new LinkedHashMap<>();
        Map<String, Long> patternTotals = new LinkedHashMap<>();
        for (Object[] row : usageRepository.sumCallsSince(from)) {
            String route = String.valueOf(row[0]);
            String method = String.valueOf(row[1]);
            long total = ((Number) row[2]).longValue();
            usageTotals.merge(method + " " + route, total, Long::sum);
            patternTotals.merge(route, total, Long::sum);
        }
        // Ajouter les compteurs mémoire pas encore vidangés
        usageService.liveCounters().forEach((k, v) -> {
            usageTotals.merge(k, v, Long::sum);
            int space = k.indexOf(' ');
            if (space > 0) {
                patternTotals.merge(k.substring(space + 1), v, Long::sum);
            }
        });

        Set<String> mapped = registeredRoutes();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> unused = new ArrayList<>();
        for (String routeKey : mapped) {
            int space = routeKey.indexOf(' ');
            String method = routeKey.substring(0, space);
            String pattern = routeKey.substring(space + 1);
            // Une route « ANY » est considérée utilisée dès qu'un appel a été
            // observé sur ce patron, quelle que soit la méthode HTTP.
            long calls = "ANY".equals(method)
                    ? patternTotals.getOrDefault(pattern, 0L)
                    : usageTotals.getOrDefault(routeKey, 0L);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("endpoint", routeKey);
            row.put("calls", calls);
            row.put("unused", calls == 0);
            rows.add(row);
            if (calls == 0) {
                unused.add(routeKey);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("windowDays", window);
        response.put("generatedAt", LocalDate.now().toString());
        response.put("mappedEndpoints", mapped.size());
        response.put("usedEndpoints", mapped.size() - unused.size());
        response.put("unusedCount", unused.size());
        response.put("unusedEndpoints", unused);
        response.put("endpoints", rows);
        return ResponseEntity.ok(response);
    }

    private Set<String> registeredRoutes() {
        Set<String> routes = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = extractPatterns(info);
            Set<String> methods = info.getMethodsCondition().getMethods().isEmpty()
                    ? Set.of("ANY")
                    : info.getMethodsCondition().getMethods().stream()
                            .map(Enum::name)
                            .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            for (String pattern : patterns) {
                for (String method : methods) {
                    routes.add(method + " " + pattern);
                }
            }
        }
        return routes;
    }

    private Set<String> extractPatterns(RequestMappingInfo info) {
        PathPatternsRequestCondition pathPatterns = info.getPathPatternsCondition();
        if (pathPatterns != null && pathPatterns.getPatterns() != null) {
            return pathPatterns.getPatterns().stream()
                    .map(org.springframework.web.util.pattern.PathPattern::getPatternString)
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        }
        return info.getPatternsCondition() != null
                ? info.getPatternsCondition().getPatterns()
                : Set.of();
    }
}
