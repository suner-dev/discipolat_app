package com.discipolat.common.multitenancy;

import com.discipolat.modules.tenants.domain.TenantStatusGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Constat B1 — application de la suspension de tenant à TOUTE requête
 * authentifiée (et non plus seulement à la connexion).
 *
 * <p>Séquence d'exécution (cf. {@code WebMvcConfig}) :
 * <pre>
 *   TenantInterceptor        pose le TenantContext depuis le JWT
 *   TenantFilterInterceptor  active le filtre Hibernate multi-tenant
 *   TenantStatusInterceptor  ⬅ CE GARDE : refuse le tenant suspendu/annulé
 *   FeatureModuleInterceptor contrôle d'accès aux modules
 * </pre>
 *
 * <p>Les chemins publics sont ignorés : ils doivent rester joignables même pour
 * un tenant suspendu — sans quoi un membre ne pourrait plus ouvrir son lien
 * d'invitation, s'authentifier pour renouveler un jeton expiré, ni voir l'état
 * de santé du service. La liste des préfixes publics n'est pas dupliquée ici :
 * elle est lue depuis {@link TenantFilter#shouldBypassFilter(HttpServletRequest)},
 * source de vérité unique.
 *
 * <p>Si aucun {@code TenantContext} n'est posé (super admin plateforme agissant
 * hors tenant, health-check), l'intercepteur est transparent : il ne doit jamais
 * transformer un accès plateforme légitime en 403.
 *
 * <p>L'exception {@code DomainException} levée ici est traitée par
 * {@code GlobalExceptionHandler} : le client reçoit un {@code ProblemDetail}
 * RFC 7807 avec {@code title = TENANT_SUSPENDED | TENANT_CANCELLED |
 * TENANT_STATUS_UNAVAILABLE} et le statut HTTP 403.
 */
@Component
@Order(1)
public class TenantStatusInterceptor implements HandlerInterceptor {

    private final ObjectProvider<TenantStatusGuard> guardProvider;
    private final ObjectProvider<TenantFilter> tenantFilterProvider;

    public TenantStatusInterceptor(ObjectProvider<TenantStatusGuard> guardProvider,
                                   ObjectProvider<TenantFilter> tenantFilterProvider) {
        this.guardProvider = guardProvider;
        this.tenantFilterProvider = tenantFilterProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true;
        }
        TenantFilter tenantFilter = tenantFilterProvider.getIfAvailable();
        if (tenantFilter != null && tenantFilter.shouldBypassFilter(request)) {
            return true;
        }
        TenantStatusGuard guard = guardProvider.getIfAvailable();
        if (guard == null) {
            // Bean TenantStatusGuard absent (tests @WebMvcTest) : dégradation gracieuse,
            // identique à celle déjà retenue pour TenantFilterInterceptor.
            return true;
        }
        guard.assertAccessible(tenantId);
        return true;
    }
}
