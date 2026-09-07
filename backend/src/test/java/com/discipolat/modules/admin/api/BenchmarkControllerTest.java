package com.discipolat.modules.admin.api;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentAssignment;
import com.discipolat.modules.departments.domain.DepartmentAssignmentRepository;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenchmarkControllerTest {

    @Mock private SoulRepository soulRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DepartmentAssignmentRepository departmentAssignmentRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private UserRepository userRepository;

    private BenchmarkController controller;
    private UUID tenantId;

    private List<Soul> souls(int count, StatutAme statut) {
        List<Soul> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Soul s = new Soul();
            s.setStatut(statut);
            s.setCreatedAt(LocalDateTime.now().minusDays(i));
            list.add(s);
        }
        return list;
    }

    @BeforeEach
    void setUp() {
        controller = new BenchmarkController(soulRepository, alertRepository, departmentRepository,
                departmentAssignmentRepository, familyRepository, eventRepository, eventRegistrationRepository,
                userRepository);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getBenchmark_ReturnsChurchDataAndAverages() {
        when(soulRepository.findByDeletedFalse()).thenReturn(souls(248, StatutAme.ACTIF));
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(5L);
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(departmentAssignmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(Collections.emptyList());

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
        when(soulRepository.findByDeletedFalse()).thenReturn(souls(200, StatutAme.ACTIF));
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(3L);
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(departmentAssignmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> body = controller.getBenchmark().getBody();
        Map<String, Object> current = (Map<String, Object>) body.get("currentChurch");

        assertEquals(200L, current.get("totalMembers"));
        assertEquals(3L, current.get("activeAlerts"));
        assertNotNull(current.get("attendanceRate"));
        assertNotNull(current.get("growthRate"));
    }

    @Test
    void getTrends_ReturnsAttendanceAndGrowthTrends() {
        when(soulRepository.findByDeletedFalse()).thenReturn(new ArrayList<>());
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(Collections.emptyList());

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
        when(soulRepository.findByDeletedFalse()).thenReturn(souls(100, StatutAme.ACTIF));
        when(alertRepository.countByStatut(StatutAlerte.ACTIVE)).thenReturn(0L);
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(departmentAssignmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> body = controller.getBenchmark().getBody();
        Map<String, Object> percentiles = (Map<String, Object>) body.get("percentile");

        assertNotNull(percentiles);
        assertTrue(percentiles.get("attendanceRate") instanceof Number);
        assertTrue(percentiles.get("growthRate") instanceof Number);
        assertTrue(percentiles.get("reportsSubmitted") instanceof Number);
        assertTrue(percentiles.get("volunteerRate") instanceof Number);
    }
}