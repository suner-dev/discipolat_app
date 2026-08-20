package com.discipolat.modules.alerts.domain;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartAlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private SmartAlertService smartAlertService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        smartAlertService = new SmartAlertService(alertRepository);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void runChecksNow_ReturnsMapWithAllCheckResults() {
        Map<String, Object> result = smartAlertService.runChecksNow();

        assertNotNull(result);
        assertTrue(result.containsKey("sustainedAbsences"));
        assertTrue(result.containsKey("noRecentContact"));
        assertTrue(result.containsKey("overdueReports"));
        assertTrue(result.containsKey("unresolvedDiscipline"));
        assertTrue(result.containsKey("inactiveDepartments"));
        assertTrue(result.containsKey("timestamp"));
    }

    @Test
    void getAnomalySummary_ReturnsCounts() {
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(10L);
        when(alertRepository.countByStatutAndPriorite(StatutAlerte.ACTIVE, "HAUTE")).thenReturn(3L);

        Map<String, Object> summary = smartAlertService.getAnomalySummary();

        assertEquals(10L, summary.get("totalActive"));
        assertEquals(3L, summary.get("criticalActive"));
        assertNotNull(summary.get("lastScan"));
    }

    @Test
    void detectSustainedAbsences_ReturnsZero_NoData() {
        int result = smartAlertService.detectSustainedAbsences(tenantId);
        assertEquals(0, result);
    }

    @Test
    void detectNoRecentContact_ReturnsZero_NoData() {
        int result = smartAlertService.detectNoRecentContact(tenantId);
        assertEquals(0, result);
    }

    @Test
    void detectOverdueReports_ReturnsZero_NoData() {
        int result = smartAlertService.detectOverdueReports(tenantId);
        assertEquals(0, result);
    }

    @Test
    void detectUnresolvedDiscipline_ReturnsZero_NoData() {
        int result = smartAlertService.detectUnresolvedDiscipline(tenantId);
        assertEquals(0, result);
    }

    @Test
    void detectInactiveDepartments_ReturnsZero_NoData() {
        int result = smartAlertService.detectInactiveDepartments(tenantId);
        assertEquals(0, result);
    }

    @Test
    void getAnomalySummary_WithZeroAlerts_ReturnsZeros() {
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(0L);
        when(alertRepository.countByStatutAndPriorite(StatutAlerte.ACTIVE, "HAUTE")).thenReturn(0L);

        Map<String, Object> summary = smartAlertService.getAnomalySummary();

        assertEquals(0L, summary.get("totalActive"));
        assertEquals(0L, summary.get("criticalActive"));
    }
}
