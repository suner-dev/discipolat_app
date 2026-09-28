package com.discipolat.common.multitenancy;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat H4 — le sélecteur d'organisation était structurellement inutilisable.
 *
 * <p>{@link TenantFilter} ajoute {@code AND tenant_id = :tenantId} à toutes les
 * requêtes de la requête HTTP, y compris au contrôle d'accès de
 * {@code switchTenant()}. Le SQL émis portait donc les DEUX prédicats — tenant
 * courant ET tenant demandé — et la bascule vers une autre église était
 * impossible.
 *
 * <p>{@link CrossTenantReadScope} est le contrepoids : il suspend le filtre le
 * temps du bloc de lecture, puis le rétablit. Ces tests verrouillent les deux
 * moitiés du contrat : le filtre est bien suspendu, et il est TOUJOURS
 * rétabli — y compris quand la lecture échoue, sinon une exception laisserait la
 * requête suivante sans isolation multi-tenant.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrossTenantReadScopeTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Session session;
    @Mock
    private Filter filter;

    private CrossTenantReadScope scopeWithEnabledFilter(UUID currentTenantId) {
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("tenantFilter")).thenReturn(filter);
        // Le retablissement fait `enableFilter(...).setParameter(...)` : sans ce
        // stub, enableFilter renvoie null et l'appel enchaine une NPE.
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        CrossTenantReadScope scope = new CrossTenantReadScope();
        ReflectionTestUtils.setField(scope, "entityManager", entityManager);
        TenantContext.setTenantId(currentTenantId);
        return scope;
    }

    @Test
    @DisplayName("H4 — le filtre est suspendu pendant la lecture, puis rétabli")
    void suspendsThenRestoresTheFilter() {
        UUID currentTenant = UUID.randomUUID();
        CrossTenantReadScope scope = scopeWithEnabledFilter(currentTenant);

        AtomicBoolean sawFilterDisabled = new AtomicBoolean(false);
        String result = scope.call(() -> {
            verify(session).disableFilter("tenantFilter");
            sawFilterDisabled.set(true);
            return "lecture cross-tenant";
        });

        assertThat(result).isEqualTo("lecture cross-tenant");
        assertThat(sawFilterDisabled).isTrue();
        // Rétablissement sur la MÊME session, avec le tenant d'origine : si le
        // tenant avait changé, le reste de la requête lirait les mauvaises lignes.
        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", currentTenant);
    }

    @Test
    @DisplayName("H4 — le filtre est rétabli même si la lecture échoue")
    void restoresTheFilterWhenTheReadFails() {
        UUID currentTenant = UUID.randomUUID();
        CrossTenantReadScope scope = scopeWithEnabledFilter(currentTenant);

        assertThatThrownBy(() -> scope.call(() -> {
            throw new IllegalStateException("échec de lecture");
        })).isInstanceOf(IllegalStateException.class);

        // Point de securite : sans ce finally, l'isolation multi-tenant serait
        // perdue pour le reste de la requete apres n'importe quelle exception.
        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", currentTenant);
    }

    @Test
    @DisplayName("H4 — sans filtre actif, on ne réactive rien (requête publique)")
    void doesNothingWhenNoFilterIsEnabled() {
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("tenantFilter")).thenReturn(null);
        CrossTenantReadScope scope = new CrossTenantReadScope();
        ReflectionTestUtils.setField(scope, "entityManager", entityManager);

        String result = scope.call(() -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(session, never()).disableFilter(anyString());
        verify(session, never()).enableFilter(anyString());
    }

    @Test
    @DisplayName("H4 — une session close pendant la lecture n'empêche pas la réponse")
    void toleratesASessionClosedDuringTheRead() {
        UUID currentTenant = UUID.randomUUID();
        when(entityManager.unwrap(Session.class))
                .thenReturn(session)                 // 1. suspension
                .thenThrow(new IllegalStateException("session closed")); // 2. retablissement
        when(session.getEnabledFilter("tenantFilter")).thenReturn(filter);
        CrossTenantReadScope scope = new CrossTenantReadScope();
        ReflectionTestUtils.setField(scope, "entityManager", entityManager);
        TenantContext.setTenantId(currentTenant);

        // Le filtre meurt avec la session : il n'y a plus rien à rétablir, et la
        // réponse doit malgré tout être rendue au client.
        assertThat(scope.call(() -> "ok")).isEqualTo("ok");
    }

    @Test
    @DisplayName("H4 — le composant ne touche à rien d'autre que le filtre")
    void touchesNothingElse() {
        CrossTenantReadScope scope = scopeWithEnabledFilter(UUID.randomUUID());
        scope.call(() -> null);
        verify(session).disableFilter("tenantFilter");
        verify(session, never()).flush();
        verify(entityManager, never()).flush();
    }
}
