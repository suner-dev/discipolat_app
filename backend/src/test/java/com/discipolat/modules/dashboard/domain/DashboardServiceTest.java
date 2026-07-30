package com.discipolat.modules.dashboard.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.domain.UserRole;
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
    private SecurityUtils securityUtils;

    private DashboardService dashboardService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                soulRepository, userRepository, familyRepository,
                makerReportRepository, familyReportRepository,
                alertRepository, soulNoteRepository,
                parallelFollowupRepository, departmentRepository,
                securityUtils
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
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
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

        var result = dashboardService.getPasteurDashboard();

        assertNotNull(result);
        assertTrue(result.containsKey("croissance"));
        assertTrue(result.containsKey("departements"));
        assertTrue(result.containsKey("familles"));
        assertTrue(result.containsKey("presences"));
    }

    @Test
    void getResponsableDashboard_WithNoDepartment_ShouldReturnMessage() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(departmentRepository.findByResponsableId(userId)).thenReturn(List.of());

        var result = dashboardService.getResponsableDashboard();

        assertNotNull(result);
        assertTrue(result.containsKey("message"));
    }

    @Test
    void getChefFamilleDashboard_WithNoFamily_ShouldReturnMessage() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
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
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
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
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
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
}
