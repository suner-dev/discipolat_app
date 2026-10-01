package com.discipolat.modules.ai.domain;

import com.discipolat.modules.tenants.domain.QuotaService;
import com.discipolat.modules.tenants.domain.TenantUsageSnapshot;
import com.discipolat.modules.tenants.domain.TenantUsageSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verrou §G6.10 (audit parité) : le tableau de bord « crédits IA » doit rester
 * tolérant quand le plan du tenant ne définit aucun quota IA (monthlyLimit == null).
 * L'ancienne implémentation utilisait {@code Map.of(...)} qui lève un NPE sur une
 * valeur null → HTTP 500 sur {@code GET /api/v1/ai/credits/dashboard}.
 *
 * <p>PORT Develop1 — le service retenu est celui de main : le quota IA provient
 * du {@link TenantUsageSnapshotService} (projection {@code snapshot.aiCredits()}),
 * et la tolérance null est garantie par l'implémentation main. Ce test valide donc
 * le contrat réel du service porté (collaborateurs de main), pas la refactorisation
 * D1 (repositories de plan) qui n'a pas été retenue.</p>
 */
@ExtendWith(MockitoExtension.class)
class AiCreditsServiceTest {

    @Mock private AiUsageRepository aiUsageRepository;
    @Mock private QuotaService quotaService;
    @Mock private TenantUsageSnapshotService usageSnapshotService;

    @InjectMocks private AiCreditsService aiCreditsService;

    /** Construit un snapshot dont seule la métrique aiCredits compte ici. */
    private static TenantUsageSnapshot snapshotWithAiCredits(long used, Long limit) {
        TenantUsageSnapshot.Metric aiCredits = new TenantUsageSnapshot.Metric(
                used, limit, null, null, null, null, null, null, false);
        return new TenantUsageSnapshot(
                null, null, null, null, null, null, null, null, aiCredits, null, null);
    }

    @Test
    void getUsageDashboard_tenantWithoutAiQuota_returnsNullSafeDashboard() {
        UUID tenantId = UUID.randomUUID();
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        // Aucun quota IA (limit == null) : chemin qui faisait planter Map.of.
        when(usageSnapshotService.getSnapshotForTenant(tenantId))
                .thenReturn(snapshotWithAiCredits(0L, null));
        when(aiUsageRepository.getTotalCreditsConsumed(tenantId, from, to)).thenReturn(null);
        when(aiUsageRepository.findByTenantIdAndUsageDateBetween(tenantId, from, to))
                .thenReturn(List.of());
        when(aiUsageRepository.getUsageByType(tenantId, from, to)).thenReturn(List.of());
        when(aiUsageRepository.getUsageByModel(tenantId, from, to)).thenReturn(List.of());

        Map<String, Object> dashboard = assertDoesNotThrow(
                () -> aiCreditsService.getUsageDashboard(tenantId, from, to));

        assertNull(dashboard.get("monthlyLimit"), "monthlyLimit doit être null, pas d'exception");
        assertNull(dashboard.get("remainingThisMonth"), "remainingThisMonth tolère null");
        assertEquals(0, dashboard.get("totalCredits"));
        assertEquals(0L, dashboard.get("totalRequests"));
        assertTrue(dashboard.containsKey("byType"));
        assertTrue(dashboard.containsKey("byModel"));
        assertTrue(dashboard.get("dailyUsage") instanceof List);
    }

    @Test
    void getUsageDashboard_tenantWithQuota_computesRemaining() {
        UUID tenantId = UUID.randomUUID();
        LocalDate from = LocalDate.now().minusDays(5);
        LocalDate to = LocalDate.now();

        // Plan PRO : limit 500, 120 consommés ce mois-ci → reste 380.
        when(usageSnapshotService.getSnapshotForTenant(tenantId))
                .thenReturn(snapshotWithAiCredits(120L, 500L));

        when(aiUsageRepository.getTotalCreditsConsumed(any(), any(), any())).thenReturn(120);
        when(aiUsageRepository.findByTenantIdAndUsageDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(aiUsageRepository.getUsageByType(any(), any(), any())).thenReturn(List.of());
        when(aiUsageRepository.getUsageByModel(any(), any(), any())).thenReturn(List.of());

        Map<String, Object> dashboard =
                aiCreditsService.getUsageDashboard(tenantId, from, to);

        assertEquals(500L, dashboard.get("monthlyLimit"));
        assertEquals(120L, dashboard.get("usedThisMonth"));
        assertEquals(380L, dashboard.get("remainingThisMonth"));
    }
}
