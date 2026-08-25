package com.discipolat.modules.dashboard.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SoulRepository soulRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private MakerReportRepository makerReportRepository;
    @Mock
    private FamilyReportRepository familyReportRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private SoulNoteRepository soulNoteRepository;
    @Mock
    private ParallelFollowupRepository parallelFollowupRepository;
    @Mock
    private com.discipolat.modules.souls.domain.SoulDepartmentRepository soulDepartmentRepository;
    @Mock
    private com.discipolat.modules.souls.domain.WorkspaceScopeService workspaceScope;
    @Mock
    private com.discipolat.modules.departments.domain.DepartmentTeamRepository departmentTeamRepository;
    @Mock
    private com.discipolat.modules.departments.domain.DepartmentPositionRepository departmentPositionRepository;
    @Mock
    private com.discipolat.modules.departments.domain.DepartmentAssignmentRepository departmentAssignmentRepository;
    @Mock
    private com.discipolat.modules.departments.domain.DepartmentTaskRepository departmentTaskRepository;
    @Mock
    private com.discipolat.modules.members.domain.MemberPresenceRepository memberPresenceRepository;
    @Mock
    private com.discipolat.modules.transfers.domain.TransferRequestRepository transferRequestRepository;
    @Mock
    private com.discipolat.modules.events.domain.EventRepository eventRepository;
    @Mock
    private com.discipolat.modules.events.domain.EventRegistrationRepository eventRegistrationRepository;
    @Mock
    private com.discipolat.modules.visits.domain.VisitRepository visitRepository;
    @Mock
    private com.discipolat.modules.prayers.domain.PrayerRepository prayerRepository;
    @Mock
    private SecurityUtils securityUtils;

    private DashboardService dashboardService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        dashboardService = new DashboardService(
                soulRepository, userRepository, familyRepository,
                makerReportRepository, familyReportRepository,
                alertRepository, soulNoteRepository,
                parallelFollowupRepository, departmentRepository,
                soulDepartmentRepository,
                departmentTeamRepository, departmentPositionRepository,
                departmentAssignmentRepository, departmentTaskRepository,
                memberPresenceRepository, transferRequestRepository,
                eventRepository, eventRegistrationRepository,
                visitRepository, prayerRepository,
                securityUtils, workspaceScope
        );
        userId = UUID.randomUUID();
    }

    @Test
    void getSummary_ShouldReturnSummaryData() {
        when(soulRepository.count()).thenReturn(45L);
        when(userRepository.countByRole(UserRole.FAISEUR)).thenReturn(8L);
        when(familyRepository.count()).thenReturn(4L);
        when(alertRepository.countByStatut(any())).thenReturn(5L);

        var result = dashboardService.getSummary();

        assertNotNull(result);
        assertEquals(45L, result.get("totalSouls"));
        assertEquals(8L, result.get("totalFaiseurs"));
        assertEquals(4L, result.get("totalFamilles"));
        assertEquals(5L, result.get("activeAlerts"));
    }

    @Test
    void getPasteurDashboard_ShouldReturnFullDashboard() {
        SecurityTestHelper.loginAs(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).role(UserRole.PASTEUR).build()
        ));
        when(departmentRepository.findAll()).thenReturn(List.of());
        when(familyRepository.findAll()).thenReturn(List.of());
        when(soulRepository.count()).thenReturn(100L);
        when(alertRepository.countByStatut(any())).thenReturn(5L);

        Page<MakerReport> emptyPage = new PageImpl<>(List.of());
        when(makerReportRepository.findBySemaine(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(emptyPage);
        when(soulRepository.findByTypeDisciple(any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(transferRequestRepository.findByStatutIn(any())).thenReturn(List.of());

        var result = dashboardService.getPasteurDashboard();

        assertNotNull(result);
        assertTrue(result.containsKey("croissance"));
        assertTrue(result.containsKey("departements"));
        assertTrue(result.containsKey("familles"));
        assertTrue(result.containsKey("presences"));
        // Le Pasteur supervise les transferts en attente de validation.
        assertTrue(result.containsKey("transfertsEnAttente"));
        assertEquals(List.of(), result.get("transfertsEnAttente"));
    }

    @Test
    void getResponsableDashboard_WithNoDepartment_ShouldReturnMessage() {
        SecurityTestHelper.loginAs(userId);
        when(departmentRepository.findByResponsableId(userId)).thenReturn(List.of());

        var result = dashboardService.getResponsableDashboard(null);

        assertNotNull(result);
        assertTrue(result.containsKey("message"));
    }

    @Test
    void getChefFamilleDashboard_WithNoFamily_ShouldReturnMessage() {
        SecurityTestHelper.loginAs(userId);
        User user = User.builder()
                .id(userId)
                .email("chef@test.com")
                .role(UserRole.FAISEUR)
                .roles(Set.of(UserRole.FAISEUR, UserRole.CHEF_DE_FAMILLE))
                .activeRole(UserRole.CHEF_DE_FAMILLE)
                .statut(UserStatus.ACTIVE)
                .estChefDeFamille(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // No familleGereeId → should return message
        var result = dashboardService.getChefFamilleDashboard(null);

        assertNotNull(result);
        assertTrue(result.containsKey("message"));
    }

    @Test
    void getCrmFaiseurDashboard_ShouldReturnDisciplesData() {
        SecurityTestHelper.loginAs(userId);
        User user = User.builder()
                .id(userId)
                .email("faiseur@test.com")
                .role(UserRole.FAISEUR)
                .roles(Set.of(UserRole.FAISEUR))
                .activeRole(UserRole.FAISEUR)
                .statut(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(soulRepository.findAllByFaiseurId(userId)).thenReturn(List.of());

        var result = dashboardService.getCrmFaiseurDashboard();

        assertNotNull(result);
        assertTrue(result.containsKey("statistiques"));
        assertTrue(result.containsKey("disciples"));
        assertTrue(result.containsKey("alertes"));

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.get("statistiques");
        assertEquals(0L, stats.get("totalDisciples"));
    }

    @Test
    void getCurrentUserMetrics_ShouldReturnUserRole() {
        SecurityTestHelper.loginAs(userId);
        User user = User.builder()
                .id(userId)
                .email("faiseur@test.com")
                .role(UserRole.FAISEUR)
                .roles(Set.of(UserRole.FAISEUR))
                .activeRole(UserRole.FAISEUR)
                .statut(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var result = dashboardService.getCurrentUserMetrics();

        assertNotNull(result);
        assertEquals("FAISEUR", result.get("role"));
    }
@Test
    void getPasteurKpis_ShouldComputeHealthScoreAndLeaderboards() {
        when(soulRepository.count()).thenReturn(50L);
        when(soulRepository.countByStatut(StatutAme.ACTIF)).thenReturn(30L);
        when(soulRepository.countByStatut(StatutAme.EN_INTEGRATION)).thenReturn(5L);
        when(soulRepository.countByStatut(StatutAme.EN_VEILLE)).thenReturn(5L);
        when(soulRepository.countByStatut(StatutAme.DECROCHE)).thenReturn(5L);
        when(alertRepository.countByStatut(any())).thenReturn(2L);
        when(familyRepository.count()).thenReturn(6L);
        when(userRepository.countByRole(UserRole.FAISEUR)).thenReturn(3L);
        when(makerReportRepository.findBySemaine(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findByRolesContaining(UserRole.FAISEUR)).thenReturn(List.of());
        when(eventRepository.findTop10ByDeletedFalseAndDateDebutAfterOrderByDateDebutAsc(any())).thenReturn(List.of());
        when(soulRepository.countByDateIntegrationBetween(any(), any())).thenReturn(3L);

        var kpis = dashboardService.getPasteurKpis();

        assertNotNull(kpis);
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) kpis.get("health");
        assertNotNull(health);
        int score = (int) health.get("score");
        assertTrue(score >= 0 && score <= 100);
        // 30/50 âmes actives → 60 % de fidélisation
        assertEquals(60.0, (double) health.get("tauxFidelisation"), 0.01);
        assertEquals(3L, health.get("nouveauxMois"));
        @SuppressWarnings("unchecked")
        Map<String, Object> resume = (Map<String, Object>) kpis.get("resume");
        assertEquals(50L, resume.get("totalAmes"));
        assertEquals(30L, resume.get("actifs"));
        assertEquals(0, ((List<?>) kpis.get("workload")).size());
        assertEquals(0, ((List<?>) kpis.get("upcomingEvents")).size());
        assertEquals(0, ((List<?>) kpis.get("overdueReports")).size());
        assertEquals(0, kpis.get("overdueReportsCount"));
    }
}
