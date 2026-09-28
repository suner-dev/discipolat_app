package com.discipolat.modules.platform.api;

import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantMembership;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construction de la représentation JSON d'une membership pour le sélecteur
 * d'organisation.
 *
 * <p><b>Constat H3.</b> Ce code utilisait {@code Map.of(...)}, qui
 * <b>interdit</b> les valeurs {@code null} et lève une
 * {@link NullPointerException} dans {@code ImmutableCollections.MapN}. Or
 * {@code scope_id} vaut {@code null} pour toute membership de portée
 * {@code TENANT} — c'est-à-dire le cas normal — et {@code plan}, {@code slug}
 * comme {@code role} peuvent l'être aussi. Le simple
 * {@code GET /api/v1/tenant-switcher/my-tenants} répondait donc 500 pour
 * <b>tout</b> utilisateur : le sélecteur d'organisation du constat B2 était
 * entièrement cassé, et rien ne le révélait car aucun test ne traversait le
 * controleur.
 *
 * <p>La logique est extraite ici pour être <b>testable sans Spring ni HTTP</b> :
 * une fonction pure, appliquée par le controleur. {@link LinkedHashMap} accepte
 * les valeurs nulles et conserve l'ordre des clés de la réponse.
 */
final class MembershipView {

    private MembershipView() {
    }

    /**
     * @param withPlan {@code true} pour la liste des églises
     *                 ({@code /my-tenants} : plan et date d'adhésion utiles),
     *                 {@code false} pour le contexte de sélection, plus compact.
     */
    static Map<String, Object> of(TenantMembership membership, Tenant tenant, boolean withPlan) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("tenantId", tenant.getId().toString());
        view.put("tenantName", tenant.getName());
        view.put("tenantSlug", tenant.getSlug());
        if (withPlan) {
            view.put("plan", tenant.getPlan());
        }
        // Le rôle est stocké deux fois : `role_id` (FK) et `role` (colonne
        // historique). On privilégie la FK résolue, mais on retombe sur la
        // colonne plutôt que d'afficher « UNKNOWN » alors que la donnée est
        // présente : `UNKNOWN` doit signifier « réellement inconnu ».
        String roleKey = membership.getRole() != null
                ? membership.getRole().getKey()
                : (membership.getRoleLegacy() != null ? membership.getRoleLegacy() : "UNKNOWN");
        view.put("role", roleKey);
        view.put("scopeType", membership.getScopeType() != null ? membership.getScopeType().name() : null);
        view.put("scopeId", membership.getScopeId() != null ? membership.getScopeId().toString() : null);
        view.put("status", membership.getStatus() != null ? membership.getStatus().name() : null);
        if (withPlan) {
            view.put("joinedAt", membership.getJoinedAt() != null ? membership.getJoinedAt().toString() : null);
        }
        return view;
    }
}
