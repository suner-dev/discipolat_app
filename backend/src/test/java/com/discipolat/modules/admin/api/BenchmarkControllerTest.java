package com.discipolat.modules.admin.api;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.members.domain.MemberService;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenchmarkControllerTest {

    @Mock private MemberService memberService;
    @Mock private AlertRepository alertRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private SecurityUtils securityUtils;

    private BenchmarkController controller;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        controller = new BenchmarkController(memberService, alertRepository, soulRepository, securityUtils);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getBenchmark_ReturnsChurchDataAndAverages() {
        when(soulRepository.count()).thenReturn(248L);
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = controller.getBenchmark();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("currentChurch"));
        assertTrue(body.containsKey("averagePeers"));
        assertTrue(body.containsKey("topQuartile"));
        assertTrue(body.containsKey("percentile"));
        assertTrue(body.containsKey("note"));
    }

    @Test
    void getBenchmark_CurrentChurchContainsExpectedKeys() {
        when(soulRepository.count()).thenReturn(200L);
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(3L);

        Map<String, Object> body = controller.getBenchmark().getBody();
        Map<String, Object> current = (Map<String, Object>) body.get("currentChurch");

        assertEquals(200L, current.get("totalMembers"));
        assertEquals(3L, current.get("activeAlerts"));
        assertNotNull(current.get("attendanceRate"));
        assertNotNull(current.get("growthRate"));
    }

    @Test
    void getTrends_ReturnsAttendanceAndGrowthTrends() {
        ResponseEntity<Map<String, Object>> response = controller.getTrends();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("attendanceTrend"));
        assertTrue(body.containsKey("growthTrend"));

        var attendanceTrend = (java.util.List<?>) body.get("attendanceTrend");
        assertFalse(attendanceTrend.isEmpty());
    }

    @Test
    void getBenchmark_PercentilesAreNumeric() {
        when(soulRepository.count()).thenReturn(100L);
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(0L);

        Map<String, Object> body = controller.getBenchmark().getBody();
        Map<String, Object> percentiles = (Map<String, Object>) body.get("percentile");

        assertNotNull(percentiles);
        assertTrue(percentiles.get("attendanceRate") instanceof Number);
        assertTrue(percentiles.get("growthRate") instanceof Number);
        assertTrue(percentiles.get("reportsSubmitted") instanceof Number);
        assertTrue(percentiles.get("volunteerRate") instanceof Number);
    }
}
