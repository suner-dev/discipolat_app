package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.UserDepartmentRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDepartmentRepository userDepartmentRepository;
    @Mock
    private MakerReportRepository makerReportRepository;
    @Mock
    private FamilyReportRepository familyReportRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SoulDepartmentRepository soulDepartmentRepository;
    @Mock
    private EntityAttachmentService attachmentService;
    @Mock
    private WorkspaceScopeService workspaceScopeService;
    @Mock
    private com.discipolat.modules.audit.domain.AuditService auditService;
    @Mock
    private com.discipolat.modules.notifications.domain.NotificationService notificationService;

    private DepartmentService service;
    private UUID currentUserId;
    private Department ownDept;
    private Department foreignDept;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(departmentRepository, familyRepository, soulRepository,
                userRepository, userDepartmentRepository, makerReportRepository,
                familyReportRepository, securityUtils, passwordEncoder, soulDepartmentRepository,
                attachmentService, workspaceScopeService, auditService, notificationService);
        currentUserId = UUID.randomUUID();
        ownDept = Department.builder().id(UUID.randomUUID())
                .nom("Département 1").responsableId(currentUserId)
                .statut(StatutEntite.ACTIVE).build();
        foreignDept = Department.builder().id(UUID.randomUUID())
                .nom("Département autre").responsableId(UUID.randomUUID())
                .statut(StatutEntite.ACTIVE).build();
    }

    @Test
    void findById_Responsable_OwnDepartment_ShouldReturn() {
        when(departmentRepository.findById(ownDept.getId())).thenReturn(Optional.of(ownDept));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(departmentRepository.findByResponsableId(currentUserId)).thenReturn(List.of(ownDept));

        Department result = service.findById(ownDept.getId());

        assertEquals(ownDept.getId(), result.getId());
    }

    @Test
    void findById_Responsable_ForeignDepartment_ShouldThrowAccessDenied() {
        when(departmentRepository.findById(foreignDept.getId())).thenReturn(Optional.of(foreignDept));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(departmentRepository.findByResponsableId(currentUserId)).thenReturn(List.of(ownDept));

        assertThrows(AccessDeniedException.class, () -> service.findById(foreignDept.getId()));
    }

    @Test
    void findAll_Responsable_ShouldOnlyReturnOwnDepartments() {
        var pageable = PageRequest.of(0, 20);
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(departmentRepository.findByResponsableId(currentUserId)).thenReturn(List.of(ownDept, foreignDept));
        when(departmentRepository.findAllByIdIn(List.of(ownDept.getId(), foreignDept.getId()), pageable))
                .thenReturn(new PageImpl<>(List.of(ownDept, foreignDept), pageable, 2));

        var result = service.findAll(pageable);

        assertEquals(2, result.getTotalElements());
        verify(departmentRepository, never()).findAll(pageable);
        verify(departmentRepository).findAllByIdIn(anyList(), eq(pageable));
    }

    @Test
    void getDepartmentKpi_ShouldUseGivenDeptId() {
        UUID soulId = UUID.randomUUID();
        UUID familleId = UUID.randomUUID();
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(departmentRepository.findById(ownDept.getId())).thenReturn(Optional.of(ownDept));
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(ownDept.getId()))
                .thenReturn(List.of(SoulDepartment.builder()
                        .soulId(soulId).departmentId(ownDept.getId()).actif(true).build()));
        when(soulRepository.findAllById(List.of(soulId))).thenReturn(List.of(Soul.builder()
                .id(soulId).nom("Nom").prenom("Prenom").familleId(familleId)
                .statut(com.discipolat.common.enums.StatutAme.ACTIF).build()));
        when(makerReportRepository.findByFaiseurIdAndSemaine(any(), any())).thenReturn(List.of());

        var kpi = service.getDepartmentKpi(ownDept.getId());

        assertNotNull(kpi);
        assertEquals(1L, kpi.get("totalMembres"));
        verify(soulDepartmentRepository, atLeastOnce()).findByDepartmentIdAndActifTrue(ownDept.getId());
    }

    @Test
    void getDepartmentKpi_NonSuperUser_ForeignDept_ShouldThrowAccessDenied() {
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(departmentRepository.findById(foreignDept.getId())).thenReturn(Optional.of(foreignDept));
        when(departmentRepository.findByResponsableId(currentUserId)).thenReturn(List.of(ownDept));

        assertThrows(AccessDeniedException.class, () -> service.getDepartmentKpi(foreignDept.getId()));
    }

    @Test
    void getDepartmentReport_ShouldIncludePiecesJointesPerFamily() {
        UUID soulId = UUID.randomUUID();
        UUID familleId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(departmentRepository.findById(ownDept.getId())).thenReturn(Optional.of(ownDept));
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(ownDept.getId()))
                .thenReturn(List.of(SoulDepartment.builder()
                        .soulId(soulId).departmentId(ownDept.getId()).actif(true).build()));
        when(soulRepository.findAllById(List.of(soulId))).thenReturn(List.of(Soul.builder()
                .id(soulId).nom("Nom").prenom("Prenom").familleId(familleId)
                .statut(com.discipolat.common.enums.StatutAme.ACTIF).build()));
        when(familyRepository.findById(familleId)).thenReturn(Optional.of(
                Family.builder().id(familleId).nom("Famille A").build()));

        FamilyReport fr = FamilyReport.builder()
                .id(reportId).familleId(familleId).chefFamilleId(UUID.randomUUID())
                .statutValidation(com.discipolat.common.enums.StatutValidation.SOUMIS)
                .build();
        when(familyReportRepository.findByFamilleIdAndSemaine(familleId, java.time.LocalDate.now()))
                .thenReturn(List.of(fr));
        when(attachmentService.itemsFor(EntityAttachment.EntityType.FAMILY_REPORT, reportId))
                .thenReturn(List.of(new EntityAttachmentService.AttachmentItem(
                        UUID.randomUUID(), UUID.randomUUID(), "Synthèse.pdf", "https://drive/1")));

        var report = service.getDepartmentReport(ownDept.getId(), java.time.LocalDate.now());

        assertNotNull(report.get("statsParFamille"));
        Map<?, ?> famStats = (Map<?, ?>) ((Map<?, ?>) report.get("statsParFamille")).get(familleId.toString());
        assertNotNull(famStats);
        assertEquals(1, ((List<?>) famStats.get("piecesJointes")).size());
        verify(attachmentService).itemsFor(EntityAttachment.EntityType.FAMILY_REPORT, reportId);
    }

    @Test
    void findById_Unknown_ShouldThrowEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(id));
    }
}
