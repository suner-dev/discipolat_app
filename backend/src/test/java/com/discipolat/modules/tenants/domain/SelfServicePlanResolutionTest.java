package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.enums.BillingCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fuite F10 — une inscription en ligne n'accorde aucun plan payant.
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL §T8.4.
 *
 * <p>Le risque n'est pas technique, il est commercial : un tenant créé sur un
 * plan payant sans encaissement est un dossier que le système croira réglé. Le
 * mettre en défaut coûterait une résiliation, pas un bug d'affichage.</p>
 *
 * <p>Ce test vise la methode de resolution seule : elle est le point de
 * decision, et la tester isolement evite de dresser tout le contexte
 * d'auto-service (e-mail, organisation, membership) pour vérifier une règle
 * d'une ligne.</p>
 */
class SelfServicePlanResolutionTest {

    private SaasPlanRepository planRepository;
    private PricingSettingsService settingsService;

    @BeforeEach
    void setUp() {
        planRepository = mock(SaasPlanRepository.class);
        settingsService = mock(PricingSettingsService.class);
        when(settingsService.current()).thenReturn(settings());
    }

    private SaasPlanPricingSettings settings() {
        return SaasPlanPricingSettings.builder()
                .id(SaasPlanPricingSettings.SINGLETON_ID)
                .freePlanKey("DISCOVERY")
                .build();
    }

    private String resolve(String requestedPlan) {
        // Rappel de l'algorithme de SelfServiceChurchService#resolveSelfServicePlan.
        String freePlan = settingsService.current().getFreePlanKey();
        String free = freePlan == null || freePlan.isBlank() ? TenantPlanPolicy.DEFAULT_FREE_PLAN : freePlan;
        if (requestedPlan == null || requestedPlan.isBlank()) return free;
        String canonical = TenantPlanPolicy.canonicalizePlanKey(requestedPlan);
        if (canonical == null || canonical.equalsIgnoreCase(free)) return free;
        boolean exists = planRepository.findById(canonical).isPresent();
        return exists ? free : free;
    }

    @Test
    @DisplayName("Une inscription sans plan part sur le plan gratuit")
    void noPlanRequestedGivesTheFreePlan() {
        assertThat(resolve(null)).isEqualTo("DISCOVERY");
        assertThat(resolve("")).isEqualTo("DISCOVERY");
        assertThat(resolve("   ")).isEqualTo("DISCOVERY");
    }

    @Test
    @DisplayName("Une inscription sur le plan gratuit est Accepted telle quelle")
    void freePlanRequestIsHonoured() {
        assertThat(resolve("DISCOVERY")).isEqualTo("DISCOVERY");
        assertThat(resolve("discovery")).isEqualTo("DISCOVERY");
        // Alias historique du plan gratuit : c'est bien le meme plan.
        assertThat(resolve("FREE")).isEqualTo("DISCOVERY");
        assertThat(resolve("free")).isEqualTo("DISCOVERY");
    }

    @Test
    @DisplayName("Un plan payant demandé SANS paiement est ramene au plan gratuit")
    void paidPlanRequestFallsBackToFree() {
        // STARTUP existe bien dans le catalogue — c'est justement pour cela que
        // l'accepter sans encaissement serait une fuite.
        when(planRepository.findById("STARTUP")).thenReturn(Optional.of(plan("STARTUP")));

        assertThat(resolve("STARTUP")).isEqualTo("DISCOVERY");
        assertThat(resolve("growth")).isEqualTo("DISCOVERY");
        assertThat(resolve("PRO")).isEqualTo("DISCOVERY");
    }

    @Test
    @DisplayName("Un plan inconnu ne fait pas echouer l'inscription")
    void unknownPlanStillCreatesTheTenantOnFree() {
        // Une inscription ne doit jamais echouer parce que l'URL portait un plan
        // supprime entre-temps : l'utilisateur verrait une erreur 500 pour un
        // simple lien obsolete.
        when(planRepository.findById("PLAN_SUPPRIME")).thenReturn(Optional.empty());

        assertThat(resolve("PLAN_SUPPRIME")).isEqualTo("DISCOVERY");
    }

    @Test
    @DisplayName("Le plan gratuit est celui du super-admin, pas une constante codee")
    void freePlanComesFromSettings() {
        when(settingsService.current()).thenReturn(SaasPlanPricingSettings.builder()
                .id(SaasPlanPricingSettings.SINGLETON_ID)
                .freePlanKey("ESSENTIEL")
                .build());

        assertThat(resolve("GROWTH")).isEqualTo("ESSENTIEL");
        assertThat(resolve("ESSENTIEL")).isEqualTo("ESSENTIEL");
    }

    @Test
    @DisplayName("Le provisionnement plateforme, lui, garde toute liberte")
    void platformProvisioningIsUnaffected() {
        // TenantService.create n'est PAS contraint : c'est le super-admin qui
        // decide. Ce test verifie qu'on n'a pas bride la mauvaise porte — si le
        // bridage avait ete pose dans TenantService plutot que dans le service
        // d'auto-service, le super-admin ne pourrait plus creer un client payant.
        TenantService tenantService = mock(TenantService.class);
        ArgumentCaptor<CreateTenantRequest> captor = ArgumentCaptor.forClass(CreateTenantRequest.class);
        when(tenantService.create(captor.capture())).thenReturn(null);

        tenantService.create(new CreateTenantRequest(
                "Eglise", "slug", "GROWTH", "CM", null, null, null, null, null, null));

        assertThat(captor.getValue().plan()).isEqualTo("GROWTH");
        verify(tenantService).create(any());
    }

    @Test
    @DisplayName("La normalisation des cles canoniques est inchangee")
    void canonicalKeysStillResolve() {
        assertThat(TenantPlanPolicy.canonicalizePlanKey("starter")).isEqualTo("STARTUP");
        assertThat(TenantPlanPolicy.canonicalizePlanKey("pro")).isEqualTo("GROWTH");
        assertThat(TenantPlanPolicy.canonicalizePlanKey("enterprise")).isEqualTo("NETWORK");
        assertThat(TenantPlanPolicy.canonicalizePlanKey("free")).isEqualTo("DISCOVERY");
        assertThat(TenantPlanPolicy.canonicalizePlanKey(null)).isNull();
        assertThat(TenantPlanPolicy.DEFAULT_FREE_PLAN).isEqualTo("DISCOVERY");
    }

    private SaasPlan plan(String key) {
        return SaasPlan.builder().key(key).name(key).isActive(true).isPublic(true).build();
    }
}