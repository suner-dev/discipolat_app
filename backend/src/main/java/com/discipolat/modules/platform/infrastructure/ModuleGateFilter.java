package com.discipolat.modules.platform.infrastructure;

import com.discipolat.modules.platform.domain.PlatformModuleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Garde-fou serveur de la modularité : les requêtes vers les API des modules
 * désactivés sont rejetées avec un 403, indépendamment du frontend.
 *
 * Le mapping préfixe d'URL → module est centralisé ici ; un module désactivé
 * est bloqué partout (Web, Mobile, requêtes directes).
 */
public class ModuleGateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ModuleGateFilter.class);

    /** Préfixe d'API → clé de module. Les API non listées restent accessibles. */
    private static final Map<String, String> PATH_TO_MODULE = pathMap();

    private final PlatformModuleRepository moduleRepository;

    /** Cache des modules désactivés, rechargé périodiquement (30 s). */
    private volatile Set<String> disabledModules = Set.of();
    private volatile long lastLoad = 0L;
    private static final long CACHE_TTL_MS = 30_000L;

    public ModuleGateFilter(PlatformModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String moduleKey = matchModule(path);
        if (moduleKey != null && isDisabled(moduleKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":403,\"title\":\"Module désactivé\","
                    + "\"detail\":\"Le module '" + moduleKey + "' est désactivé par l'administration.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDisabled(String moduleKey) {
        long now = System.currentTimeMillis();
        if (now - lastLoad > CACHE_TTL_MS) {
            try {
                disabledModules = moduleRepository.findByEnabledFalse().stream()
                        .map(m -> m.getKey().toUpperCase())
                        .collect(Collectors.toSet());
                lastLoad = now;
            } catch (Exception e) {
                log.warn("ModuleGateFilter: cache reload failed, using stale data", e);
            }
        }
        return disabledModules.contains(moduleKey.toUpperCase());
    }

    private static String matchModule(String path) {
        // Correspondance la plus longue d'abord (ex : /souls/retractions → SOULS).
        // Les clés « PREFIXE@@SOUS_MODULE » ciblent un sous-chemin après un segment
        // variable (UUID) : /api/v1/departments/{id}/equipment → DEPT_INVENTORY.
        String best = null;
        int bestLen = -1;
        for (Map.Entry<String, String> e : PATH_TO_MODULE.entrySet()) {
            String pattern = e.getKey();
            int len = -1;
            int idx = pattern.indexOf("@@");
            if (idx >= 0) {
                String prefix = pattern.substring(0, idx);
                String tool = pattern.substring(idx + 2);
                if (path.startsWith(prefix)) {
                    String rest = path.substring(prefix.length());
                    if (rest.matches("[^/]+/" + tool + "(/.*)?$")) {
                        len = prefix.length() * 1000 + tool.length();
                    }
                }
            } else if (path.startsWith(pattern)) {
                len = pattern.length();
            }
            if (len > bestLen) {
                bestLen = len;
                best = e.getValue();
            }
        }
        return best;
    }

    private static Map<String, String> pathMap() {
        Map<String, String> m = new HashMap<>();
        m.put("/api/v1/souls", "SOULS");
        m.put("/api/v1/families", "FAMILIES");
        m.put("/api/v1/departments", "DEPARTMENTS");
        // Sous-outils du DMS (modulables individuellement)
        m.put("/api/v1/departments/@@reports", "DEPT_REPORTS");
        m.put("/api/v1/departments/@@checklists", "DEPT_CHECKLISTS");
        m.put("/api/v1/departments/@@equipment", "DEPT_INVENTORY");
        m.put("/api/v1/departments/@@documents", "DEPT_DOCUMENTS");
        m.put("/api/v1/reports", "REPORTS");
        m.put("/api/v1/prayers", "PRAYERS");
        m.put("/api/v1/events", "EVENTS");
        m.put("/api/v1/transfers", "TRANSFERS");
        m.put("/api/v1/documents", "DOCUMENTS");
        m.put("/api/v1/files", "DOCUMENTS");
        m.put("/api/v1/alerts", "ALERTS");
        m.put("/api/v1/evaluations", "EVALUATIONS");
        m.put("/api/v1/trainings", "TRAININGS");
        m.put("/api/v1/finances", "FINANCES");
        m.put("/api/v1/badges", "BADGES");
        m.put("/api/v1/appointments", "APPOINTMENTS");
        m.put("/api/v1/messages", "MESSAGES");
        m.put("/api/v1/conversations", "MESSAGES");
        m.put("/api/v1/users", "USERS");
        m.put("/api/v1/audit", "AUDIT");
        m.put("/api/v1/permissions", "PERMISSIONS");
        m.put("/api/v1/map", "MAP");
        m.put("/api/v1/evangelism", "EVANGELISM");
        m.put("/api/v1/objectives", "OBJECTIVES");
        m.put("/api/v1/visits", "VISITS");
        m.put("/api/v1/parallel-followups", "PARALLEL_FOLLOWUPS");
        m.put("/api/v1/members", "MEMBER_REQUESTS");
        m.put("/api/v1/settings", "SETTINGS");
        m.put("/api/v1/crm", "CRM_FAISEUR");
        m.put("/api/v1/search", "SEARCH");
        m.put("/api/v1/interactions", "CRM_FAISEUR");
        m.put("/api/v1/programs", "EVENTS");
        return Map.copyOf(m);
    }
}
