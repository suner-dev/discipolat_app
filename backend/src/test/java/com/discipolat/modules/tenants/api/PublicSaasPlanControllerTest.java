package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicSaasPlanControllerTest {

    @Test
    void exposesOnlyTheFourCanonicalPlansWithPublicCacheHeaders() {
        SaasPlanService service = mock(SaasPlanService.class);
        when(service.getPublicPlans()).thenReturn(List.of(
                plan("NETWORK", 4), plan("FREE", 0), plan("DISCOVERY", 1),
                plan("GROWTH", 3), plan("STARTUP", 2)));
        PublicSaasPlanController controller = new PublicSaasPlanController(service);

        var response = controller.getPublicPlans();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getCacheControl()).contains("public", "max-age=300", "must-revalidate");
        assertThat(response.getBody()).extracting(PublicSaasPlanController.PublicPlanResponse::key)
                .containsExactly("DISCOVERY", "STARTUP", "GROWTH", "NETWORK");
        assertThat(response.getBody()).allSatisfy(plan -> {
            assertThat(plan.name()).isNotBlank();
            assertThat(plan.currency()).isNotBlank();
        });
    }

    private SaasPlan plan(String key, int sortOrder) {
        return SaasPlan.builder().key(key).name(key).description("Public").currency("EUR")
                .priceMonthly(0L).priceYearly(0L).priceEur(0L).isActive(true).isPublic(true)
                .seatsLimit(50).storageLimitMb(500).aiCreditsLimit(100).billingPeriod("monthly")
                .limitsJson("{\"internal\":true}").featuresJson("{\"internal\":true}")
                .sortOrder(sortOrder).build();
    }
}
