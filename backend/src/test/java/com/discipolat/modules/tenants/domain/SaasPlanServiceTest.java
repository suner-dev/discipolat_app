package com.discipolat.modules.tenants.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaasPlanServiceTest {

    @Test
    void publicCatalogContainsOnlyCanonicalPlansInCanonicalOrder() {
        SaasPlanRepository repository = mock(SaasPlanRepository.class);
        when(repository.findByIsActiveTrueAndIsPublicTrueOrderBySortOrderAsc()).thenReturn(List.of(
                plan("NETWORK", 4), plan("FREE", 0), plan("STARTUP", 2),
                plan("DISCOVERY", 1), plan("GROWTH", 3)));
        SaasPlanService service = new SaasPlanService(repository,
                mock(TenantSubscriptionRepository.class), mock(TenantRepository.class));

        assertThat(service.getPublicPlans()).extracting(SaasPlan::getKey)
                .containsExactly("DISCOVERY", "STARTUP", "GROWTH", "NETWORK");
    }

    private SaasPlan plan(String key, int sortOrder) {
        return SaasPlan.builder().key(key).isActive(true).isPublic(true).status("ACTIVE")
                .sortOrder(sortOrder).build();
    }
}
