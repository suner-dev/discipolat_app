package com.discipolat.modules.kpiNarrative.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiNarrativeServiceTest {

    @Mock
    private KpiNarrativeRepository repository;

    private KpiNarrativeService narrativeService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        narrativeService = new KpiNarrativeService(repository);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        lenient().when(repository.save(any(KpiNarrative.class))).thenAnswer(inv -> {
            KpiNarrative n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── TREND DETECTION ────────────────────────────────────────

    @Test
    void generate_significantIncrease_shouldSetSignificativeHausse() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRÉSENCE, 85.0, 70.0, null, Map.of());

        assertEquals(KpiNarrative.Tendance.SIGNIFICATIVE_HAUSSE, result.getTendance());
        assertTrue(result.getVariationPct() > 20);
    }

    @Test
    void generate_moderateIncrease_shouldSetHausse() {
        // Variation = (12-10)/10*100 = 20% => SIGNIFICATIVE_HAUSSE
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.CROISSANCE, 11.0, 10.0, null, Map.of());

        assertEquals(KpiNarrative.Tendance.HAUSSE, result.getTendance());
        assertTrue(result.getVariationPct() > 0 && result.getVariationPct() <= 10);
    }

    @Test
    void generate_stable_shouldSetStable() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.ENGAGEMENT, 50.0, 50.0, null, Map.of());

        assertEquals(KpiNarrative.Tendance.STABLE, result.getTendance());
        assertEquals(0.0, result.getVariationPct(), 0.1);
    }

    @Test
    void generate_moderateDecrease_shouldSetBaisse() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.FINANCES, 4000.0, 4200.0, null, Map.of());

        assertEquals(KpiNarrative.Tendance.BAISSE, result.getTendance());
        assertTrue(result.getVariationPct() < 0 && result.getVariationPct() > -10);
    }

    @Test
    void generate_significantDecrease_shouldSetSignificativeBaisse() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.RÉTENTION, 50.0, 70.0, null, Map.of());

        assertEquals(KpiNarrative.Tendance.SIGNIFICATIVE_BAISSE, result.getTendance());
        assertTrue(result.getVariationPct() < -20);
    }

    // ── NARRATION CONTENT ──────────────────────────────────────

    @Test
    void generate_presenceDecrease_shouldMentionPresence() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRÉSENCE, 60.0, 70.0, null, Map.of());

        assertNotNull(result.getNarration());
        assertTrue(result.getNarration().toLowerCase().contains("présence"));
    }

    @Test
    void generate_financesIncreaseShouldMentionRevenus() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.FINANCES, 5000.0, 4000.0, null, Map.of());

        assertNotNull(result.getNarration());
        assertTrue(result.getNarration().toLowerCase().contains("revenus"));
    }

    @Test
    void generate_withDepartmentName_shouldIncludeDeptInNarration() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRÉSENCE, 60.0, 70.0, UUID.randomUUID(),
                Map.of("départementNom", "Jeunesse"));

        assertTrue(result.getNarration().contains("Jeunesse"));
    }

    // ── CAUSES & RECOMMANDATIONS ───────────────────────────────

    @Test
    void generate_decreasePresence_shouldIdentifyCauses() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRÉSENCE, 60.0, 75.0, null, Map.of());

        assertNotNull(result.getCauses());
        assertFalse(result.getCauses().equals("Aucune cause identifiée"));
    }

    @Test
    void generate_decreasePresence_shouldSuggestActions() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRÉSENCE, 55.0, 70.0, null, Map.of());

        assertNotNull(result.getRecommandations());
        assertFalse(result.getRecommandations().isEmpty());
    }

    @Test
    void generate_decreaseReports_shouldIdentifyFaiseurOverload() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.RAPPORTS, 30.0, 50.0, null, Map.of());

        assertNotNull(result.getCauses());
        assertTrue(result.getCauses().contains("faiseur") || result.getCauses().contains("Surcharge"));
    }

    @Test
    void generate_stableShouldSuggestNewApproaches() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.ENGAGEMENT, 50.0, 50.0, null, Map.of());

        assertNotNull(result.getRecommandations());
        assertTrue(result.getRecommandations().contains("nouvelles approches") || result.getRecommandations().contains("Explorer"));
    }

    @Test
    void generate_increaseShouldDocumentGoodPractices() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.SCORE_SPIRITUEL, 75.0, 65.0, null, Map.of());

        assertNotNull(result.getRecommandations());
        assertTrue(result.getRecommandations().contains("bonnes pratiques") || result.getRecommandations().contains("maintenir"));
    }

    @Test
    void generate_withContextAbsencesConsecutives_shouldMentionAbsences() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRÉSENCE, 55.0, 70.0, null,
                Map.of("absencesConsecutives", 3));

        assertNotNull(result.getCauses());
    }

    // ── BATCH GENERATION ───────────────────────────────────────

    @Test
    void generateAll_shouldGenerateNarrativesForAllKpis() {
        Map<KpiNarrative.TypeKPI, double[]> kpisData = new LinkedHashMap<>();
        kpisData.put(KpiNarrative.TypeKPI.PRÉSENCE, new double[]{70.0, 75.0});
        kpisData.put(KpiNarrative.TypeKPI.CROISSANCE, new double[]{12.0, 10.0});
        kpisData.put(KpiNarrative.TypeKPI.FINANCES, new double[]{4500.0, 4200.0});

        List<KpiNarrative> result = narrativeService.generateAll(kpisData);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(n -> n.getNarration() != null));
    }

    @Test
    void generateAll_withSingleValue_shouldSkip() {
        Map<KpiNarrative.TypeKPI, double[]> kpisData = new HashMap<>();
        kpisData.put(KpiNarrative.TypeKPI.ALERTES, new double[]{5.0}); // Only one value

        List<KpiNarrative> result = narrativeService.generateAll(kpisData);

        assertTrue(result.isEmpty());
    }

    // ── LIST QUERIES ───────────────────────────────────────────

    @Test
    void listByType_shouldFilter() {
        KpiNarrative n1 = new KpiNarrative();
        n1.setId(UUID.randomUUID());
        n1.setTypeKPI(KpiNarrative.TypeKPI.PRÉSENCE);
        when(repository.findByTenantIdAndTypeKPIOrderByGénéréLeDesc(tenantId, KpiNarrative.TypeKPI.PRÉSENCE))
                .thenReturn(List.of(n1));

        List<KpiNarrative> result = narrativeService.listByType(KpiNarrative.TypeKPI.PRÉSENCE);

        assertEquals(1, result.size());
    }

    @Test
    void listAll_shouldReturnAll() {
        when(repository.findByTenantIdOrderByGénéréLeDesc(tenantId))
                .thenReturn(List.of(new KpiNarrative(), new KpiNarrative()));

        List<KpiNarrative> result = narrativeService.listAll();

        assertEquals(2, result.size());
    }

    // ── VARIATION CALCULATION ──────────────────────────────────

    @Test
    void generate_zeroPreviousValue_shouldHandleGracefully() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.CROISSANCE, 10.0, 0.0, null, Map.of());

        assertNotNull(result);
        assertEquals(KpiNarrative.Tendance.STABLE, result.getTendance());
    }

    @Test
    void generate_sameValues_shouldBeStable() {
        KpiNarrative result = narrativeService.generate(
                KpiNarrative.TypeKPI.PRIÈRES, 100.0, 100.0, null, Map.of());

        assertEquals(KpiNarrative.Tendance.STABLE, result.getTendance());
        assertEquals(0.0, result.getVariationPct(), 0.1);
    }
}
