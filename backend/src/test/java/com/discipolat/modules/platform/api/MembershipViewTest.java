package com.discipolat.modules.platform.api;

import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantMembership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Constat H3 — {@code GET /api/v1/tenant-switcher/my-tenants} renvoyait 500 pour
 * TOUT utilisateur.
 *
 * <p>Cause : la réponse était construite avec {@code Map.of(...)}, qui <b>interdit</b>
 * les valeurs {@code null}, alors que {@code scope_id} vaut {@code null} pour
 * toute membership de portée {@code TENANT} — le cas normal, pas une exception.
 * Le {@code NullPointerException} rendait le sélecteur d'organisation inutilisable,
 * donc le constat B2 du plan non delivered.
 *
 * <p>Ces tests verrouillent le comportement sans Spring ni HTTP : c'est le seul
 * endroit où ce piège peut se manifestations, et il était invisible jusqu'ici.
 */
class MembershipViewTest {

    private static TenantMembership membershipWithNullScopeId() {
        return TenantMembership.builder()
                .tenantId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .roleLegacy("TENANT_ADMIN")
                .status(MembershipStatus.ACTIVE)
                .scopeType(MembershipScopeType.TENANT)
                .scopeId(null)   // <- cas normal d'une membership de portée TENANT
                .joinedAt(Instant.parse("2026-09-28T10:00:00Z"))
                .build();
    }

    private static Tenant tenant() {
        return Tenant.builder()
                .id(UUID.randomUUID())
                .name("Eglise de Douala")
                .slug("eglise-de-douala")
                .plan("DISCOVERY")
                .build();
    }

    @Test
    @DisplayName("H3 — le piege est bien reel : Map.of refuse une valeur nulle")
    void mapOfReallyRejectsNullValues() {
        // Ce test n'est pas decoratif : il demontre que le correctif n'est pas
        // superflu. Si Map.of tolerait un jour le null, ce test le signalerait et
        // le code pourrait revenir a la forme lisible.
        assertThatThrownBy(() -> Map.of("scopeId", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("H3 — une membership de portée TENANT (scopeId nul) est sérialisable")
    void tenantScopedMembershipWithNullScopeIdIsRendered() {
        Map<String, Object> view = MembershipView.of(membershipWithNullScopeId(), tenant(), true);

        assertThat(view).containsEntry("tenantName", "Eglise de Douala");
        // role_id non resolue -> repli sur la colonne `role`, qui porte la meme
        // information. "UNKNOWN" doit rester reserve au cas vraiment inconnu.
        assertThat(view).containsEntry("role", "TENANT_ADMIN");
        assertThat(view).containsEntry("status", "ACTIVE");
        assertThat(view).containsEntry("scopeType", "TENANT");
        assertThat(view).containsKey("scopeId");
        assertThat(view.get("scopeId")).isNull();
    }

    @Test
    @DisplayName("H3 — aucune valeur nulle ne fait échouer la construction de la vue")
    void neverThrowsOnMissingOptionalData() {
        Tenant sparse = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Eglise sans slug")
                .build();   // slug et plan absents
        TenantMembership sparseMembership = TenantMembership.builder()
                .tenantId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(MembershipStatus.ACTIVE)
                .scopeType(MembershipScopeType.TENANT)
                .build();  // ni role, ni joinedAt

        assertThatCode(() -> MembershipView.of(sparseMembership, sparse, true))
                .doesNotThrowAnyException();

        Map<String, Object> view = MembershipView.of(sparseMembership, sparse, true);
        assertThat(view).containsEntry("role", "UNKNOWN");   // repli explicite
        assertThat(view.get("tenantSlug")).isNull();
        assertThat(view.get("joinedAt")).isNull();
    }

    @Test
    @DisplayName("H3 — l'ordre des clés de la réponse est conservé (contrat de forme)")
    void keepsTheDocumentedKeyOrder() {
        assertThat(MembershipView.of(membershipWithNullScopeId(), tenant(), true).keySet())
                .containsExactly("tenantId", "tenantName", "tenantSlug", "plan", "role",
                        "scopeType", "scopeId", "status", "joinedAt");
    }

    @Test
    @DisplayName("H3 — la vue de sélection est plus compacte (ni plan ni joinedAt)")
    void selectionViewIsMoreCompact() {
        assertThat(MembershipView.of(membershipWithNullScopeId(), tenant(), false).keySet())
                .containsExactly("tenantId", "tenantName", "tenantSlug", "role",
                        "scopeType", "scopeId", "status");
    }
}
