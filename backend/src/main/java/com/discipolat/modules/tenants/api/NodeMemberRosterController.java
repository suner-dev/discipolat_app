package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Roster des membres d'un nœud — « consulter les membres à chaque échelle ».
 *
 * <p>Complète l'agrégat V3 « nombres sans PII » ({@code /aggregate}, compteurs
 * de l'arbre) par la LISTE nominative des membres relevant d'un nœud, en
 * distinguant les rattachements <b>directs</b> des rattachements <b>indirects</b>
 * (sous-structures), dédupliqués et paginés.
 *
 * <h2>Garde d'accès</h2>
 * {@code isAuthenticated()} — stricte parité avec l'annuaire existant
 * {@code GET /api/v1/users} que tout membre authentifié du tenant consulte déjà
 * (web et mobile) : cette lecture n'ouvre donc AUCUNE donnée qui n'était pas
 * déjà visible dans l'annuaire, elle la <b>scopé</b> sur une entité. L'écriture
 * de roster n'existe pas ici (lecture seule). Le tenant provient du seul
 * contexte serveur ; le nœud est revérifié dans le tenant (404 cross-tenant,
 * jamais 403 — pas d'énumération).
 *
 * <h2>Contrat</h2>
 * <pre>
 * GET /api/v1/tenant/organization/nodes/{nodeId}/members
 *     ?scope=DIRECT|SUBTREE   (défaut SUBTREE)
 *     &amp;q=nom|email           (filtre libre, insensible à la casse)
 *     &amp;page=0&amp;size=20         (size borné 100)
 * → { nodeId, nodeName, nodeType, scope, content[NodeMemberRow],
 *     page, size, totalElements, totalPages,
 *     memberCountDirect, memberCountSousArbre }
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/tenant/organization")
public class NodeMemberRosterController {

    private final NodeMemberRosterService rosterService;

    public NodeMemberRosterController(NodeMemberRosterService rosterService) {
        this.rosterService = rosterService;
    }

    @GetMapping("/nodes/{nodeId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> members(@PathVariable UUID nodeId,
                                          @RequestParam(defaultValue = "SUBTREE") String scope,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.requireTenantId();
        NodeMemberRosterService.RosterScope rosterScope;
        try {
            rosterScope = NodeMemberRosterService.RosterScope.valueOf(
                    scope == null ? "SUBTREE" : scope.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            rosterScope = NodeMemberRosterService.RosterScope.SUBTREE;
        }
        return ResponseEntity.ok(rosterService.roster(tenantId, nodeId, rosterScope, q, page, size));
    }
}
