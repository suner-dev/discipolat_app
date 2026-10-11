package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Régression de la faille F3 — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T3.1.
 *
 * <p>Le catalogue public était filtré deux fois par
 * {@code SaasPlanService.PUBLIC_PLAN_KEYS}, une liste fermée de quatre plans.
 * Conséquence : <b>un plan créé par le super-admin n'apparaissait jamais</b> sur
 * la page des tarifs, même avec {@code is_public = true}. La fonction
 * « créer une offre » était donc inopérante alors que l'interface la proposait.
 * Ces tests verrouillent la correction : la seule condition est « publié et
 * actif », et la liste canonique ne sert plus qu'à l'ordre d'affichage.</p>
 */
class PublicSaasPlanControllerTest {

    @Test
    @DisplayName("Un plan publié par le super-admin reste visible, quel que soit son ordre")
    void exposesPublishedPlansWhateverTheirKey() {
        SaasPlanService service = mock(SaasPlanService.class);
        when(service.getPublicPlans()).thenReturn(List.of(
                plan("NETWORK", 4), plan("PREMIUM", 5), plan("DISCOVERY", 1),
                plan("GROWTH", 3), plan("STARTUP", 2)));

        var response = new PublicSaasPlanController(service).getPublicPlans();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getCacheControl()).contains("public", "max-age=300", "must-revalidate");

        // Les quatre plans canoniques gardent leur position historique…
        assertThat(response.getBody()).extracting(PublicSaasPlanController.PublicPlanResponse::key)
                .containsExactly("DISCOVERY", "STARTUP", "GROWTH", "NETWORK", "PREMIUM");
        assertThat(response.getBody()).allSatisfy(plan -> {
            assertThat(plan.name()).isNotBlank();
            assertThat(plan.currency()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Les champs historiques du contrat restent exposés")
    void legacyContractFieldsStillPresent() {
        SaasPlanService service = mock(SaasPlanService.class);
        when(service.getPublicPlans()).thenReturn(List.of(plan("GROWTH", 3)));

        var body = new PublicSaasPlanController(service).getPublicPlans().getBody();

        // Ces champs ont une sémantique de montant MAJEUR héritée : les retirer
        // casserait le client mobile et les intégrations tierces.
        assertThat(body).singleElement().satisfies(plan -> {
            assertThat(plan.priceMonthly()).isEqualTo(0L);
            assertThat(plan.priceEur()).isEqualTo(0L);
            assertThat(plan.currency()).isEqualTo("EUR");
            assertThat(plan.billingPeriod()).isEqualTo("monthly");
            assertThat(plan.trialDays()).isEqualTo(30);
            assertThat(plan.annualDiscountPct()).isEqualTo(17);
        });
    }

    @Test
    @DisplayName("Un plan sans clé exploitable ne casse pas la réponse")
    void toleratesMissingKey() {
        SaasPlanService service = mock(SaasPlanService.class);
        when(service.getPublicPlans()).thenReturn(List.of(plan("GROWTH", 3)));

        assertThat(new PublicSaasPlanController(service).getPublicPlans().getBody()).hasSize(1);
    }

    private SaasPlan plan(String key, int sortOrder) {
        return SaasPlan.builder().key(key).name(key).description("Public").currency("EUR")
                .priceMonthly(0L).priceYearly(0L).priceEur(0L).isActive(true).isPublic(true)
                .seatsLimit(50).storageLimitMb(500).aiCreditsLimit(100).billingPeriod("monthly")
                .limitsJson("{\"internal\":true}").featuresJson("{\"internal\":true}")
                .sortOrder(sortOrder).build();
    }
}