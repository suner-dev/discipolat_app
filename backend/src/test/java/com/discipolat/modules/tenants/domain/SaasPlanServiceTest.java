package com.discipolat.modules.tenants.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Régression de la faille F3 — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T3.1.
 *
 * <p>« Catalogue public = quatre clés en dur » rendait toute création de plan
 * inopérante. Le service ne filtre plus sur une liste fermée : seuls l'activation,
 * la publication et le statut comptent.</p>
 */
class SaasPlanServiceTest {

    @Test
    @DisplayName("Le catalogue public accepte tout plan publié, pas seulement les quatre canoniques")
    void publicCatalogAcceptsAnyPublishedPlan() {
        SaasPlanRepository repository = mock(SaasPlanRepository.class);
        when(repository.findByIsActiveTrueAndIsPublicTrueOrderBySortOrderAsc()).thenReturn(List.of(
                plan("NETWORK", 4), plan("PREMIUM", 5), plan("STARTUP", 2),
                plan("DISCOVERY", 1), plan("GROWTH", 3)));

        SaasPlanService service = new SaasPlanService(repository,
                mock(TenantSubscriptionRepository.class), mock(TenantRepository.class));

        assertThat(service.getPublicPlans()).extracting(SaasPlan::getKey)
                .containsExactlyInAnyOrder("DISCOVERY", "STARTUP", "GROWTH", "NETWORK", "PREMIUM");
    }

    @Test
    @DisplayName("Un plan déprécié ou inactif reste hors du catalogue")
    void deprecatedPlansAreExcluded() {
        SaasPlanRepository repository = mock(SaasPlanRepository.class);
        when(repository.findByIsActiveTrueAndIsPublicTrueOrderBySortOrderAsc()).thenReturn(List.of(
                plan("GROWTH", 3), plan("OLD", 9, "DEPRECATED"), plan("STOPPED", 10, "INACTIVE")));

        SaasPlanService service = new SaasPlanService(repository,
                mock(TenantSubscriptionRepository.class), mock(TenantRepository.class));

        assertThat(service.getPublicPlans()).extracting(SaasPlan::getKey).containsExactly("GROWTH");
    }

    private SaasPlan plan(String key, int sortOrder) {
        return plan(key, sortOrder, "ACTIVE");
    }

    private SaasPlan plan(String key, int sortOrder, String status) {
        return SaasPlan.builder().key(key).isActive(true).isPublic(true).status(status)
                .sortOrder(sortOrder).build();
    }
}