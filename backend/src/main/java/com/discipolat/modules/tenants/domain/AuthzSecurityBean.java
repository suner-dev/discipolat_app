package com.discipolat.modules.tenants.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bean nommé {@code authz} utilisable dans les expressions de sécurité SpEL :
 * {@code @PreAuthorize("@authz.can('ROLE_CREATE', 'TENANT', null)")}.
 *
 * Délègue à {@link AuthorizationService} en résolvant l'utilisateur et le tenant
 * courants depuis le contexte d'exécution. Ne lève jamais d'exception : tout
 * contexte absent ou valeur invalide est traduit en autorisation refusée.
 */
@Component("authz")
public class AuthzSecurityBean {

    private final AuthorizationService authzService;

    public AuthzSecurityBean(AuthorizationService authzService) {
        this.authzService = authzService;
    }

    public boolean can(String permissionKey, String scopeType, Object scopeId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.getTenantId();
        if (userId == null || tenantId == null) {
            return false;
        }
        try {
            MembershipScopeType scope = MembershipScopeType.valueOf(scopeType.toUpperCase());
            UUID scopeUuid = scopeId != null ? UUID.fromString(scopeId.toString()) : null;
            return authzService.can(userId, tenantId, permissionKey, scope, scopeUuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Super administrateur plateforme (rôle global {@code PLATFORM_SUPER_ADMIN}).
     *
     * <p>Exposé ici pour que l'expression {@code @PreAuthorize("@authz.isPlatformSuperAdmin()")}
     * — utilisée notamment par {@code ImpersonationController} et l'export de tenant G4.5 —
     * soit résolvable au runtime (le bean {@code authz} est {@link AuthzSecurityBean}).
     * Ne lève jamais d'exception : tout contexte absent vaut refus.
     */
    public boolean isPlatformSuperAdmin() {
        try {
            UUID userId = SecurityUtils.getCurrentUserId();
            return userId != null && authzService.isPlatformSuperAdmin(userId);
        } catch (Exception e) {
            return false;
        }
    }
}