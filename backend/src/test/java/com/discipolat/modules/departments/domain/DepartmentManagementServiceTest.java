package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.api.DepartmentAssignmentRequest;
import com.discipolat.modules.departments.api.DepartmentTaskRequest;
import com.discipolat.modules.departments.api.DepartmentTeamRequest;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentManagementServiceTest {

    @Mock
    private DepartmentService departmentService;
    @Mock
    private DepartmentTeamRepository teamRepository;
    @Mock
    private DepartmentPositionRepository positionRepository;
    @Mock
    private DepartmentAssignmentRepository assignmentRepository;
    @Mock
    private DepartmentTaskRepository taskRepository;
    @Mock
    private DepartmentActivityRepository activityRepository;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SoulDepartmentRepository soulDepartmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SoulService soulService;

    private DepartmentManagementService service;
    private final UUID deptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DepartmentManagementService(departmentService, teamRepository, positionRepository,
                assignmentRepository, taskRepository, activityRepository, soulRepository,
                soulDepartmentRepository, userRepository, securityUtils, notificationService, soulService);
        // L'accès au département est toujours accordé par défaut (assertCanManage)
        lenient().when(departmentService.findById(deptId)).thenReturn(new Department());
        // Identité de l'acteur pour le journal d'activité
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        lenient().when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        // Département sans membres rattachés → constitution libre (règle tolérante)
        lenient().when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of());
    }

    // ======================= ÉQUIPES =======================

    @Test
    void createTeam_savesTeamAndRecordsActivity() {
        DepartmentTeamRequest request = new DepartmentTeamRequest("Son", null,
                DepartmentTeam.TeamType.EQUIPE_PERMANENTE, null, null, "Assurer le son",
                null, null, null);
        UUID teamId = UUID.randomUUID();

        when(teamRepository.save(any(DepartmentTeam.class))).thenAnswer(inv -> {
            DepartmentTeam t = inv.getArgument(0);
            t.setId(teamId);
            return t;
        });
        when(assignmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of());

        Map<String, Object> result = service.createTeam(deptId, request);

        assertThat(result.get("id")).isEqualTo(teamId);
        assertThat(result.get("nom")).isEqualTo("Son");
        verify(teamRepository).save(any(DepartmentTeam.class));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void createTeam_rejectsParentFromAnotherDepartment() {
        UUID otherDept = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        DepartmentTeam parent = DepartmentTeam.builder().id(parentId).departmentId(otherDept).build();
        when(teamRepository.findById(parentId)).thenReturn(Optional.of(parent));

        DepartmentTeamRequest request = new DepartmentTeamRequest("Enfant", parentId,
                DepartmentTeam.TeamType.SOUS_DEPARTEMENT, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.createTeam(deptId, request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateTeam_rejectsSelfParentCycle() {
        UUID teamId = UUID.randomUUID();
        DepartmentTeam team = DepartmentTeam.builder().id(teamId).departmentId(deptId).nom("Vidéo").build();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        DepartmentTeamRequest request = new DepartmentTeamRequest("Vidéo", teamId,
                DepartmentTeam.TeamType.SOUS_DEPARTEMENT, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateTeam(deptId, teamId, request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void archiveTeam_archivesRecursively() {
        UUID teamId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        DepartmentTeam team = DepartmentTeam.builder().id(teamId).departmentId(deptId).nom("Vidéo").build();
        DepartmentTeam child = DepartmentTeam.builder().id(childId).departmentId(deptId).parentId(teamId).nom("Montage").build();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.findByParentId(teamId)).thenReturn(List.of(child));
        when(teamRepository.findByParentId(childId)).thenReturn(List.of());

        service.archiveTeam(deptId, teamId);

        assertThat(team.getStatut()).isEqualTo(DepartmentTeam.TeamStatus.ARCHIVED);
        assertThat(child.getStatut()).isEqualTo(DepartmentTeam.TeamStatus.ARCHIVED);
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    // ======================= AFFECTATIONS =======================

    @Test
    void assignMember_savesWithActiveFlagAndRecordsActivity() {
        UUID memberId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Soul soul = mock(Soul.class);
        when(soul.getNomComplet()).thenReturn("Jean Dupont");
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));

        DepartmentTeam team = DepartmentTeam.builder().id(teamId).departmentId(deptId).nom("Son").build();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(assignmentRepository.existsByMemberIdAndTeamIdAndActifTrue(memberId, teamId)).thenReturn(false);

        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.save(any(DepartmentAssignment.class))).thenAnswer(inv -> {
            DepartmentAssignment a = inv.getArgument(0);
            a.setId(assignmentId);
            return a;
        });

        DepartmentAssignmentRequest request = new DepartmentAssignmentRequest(memberId, teamId, null,
                DepartmentAssignment.AssignmentRole.MEMBRE, LocalDate.now(), null);

        Map<String, Object> result = service.assignMember(deptId, request);

        assertThat(result.get("id")).isEqualTo(assignmentId);
        assertThat(result.get("memberNom")).isEqualTo("Jean Dupont");
        assertThat(result.get("teamNom")).isEqualTo("Son");
        assertThat(result.get("actif")).isEqualTo(true);
        verify(assignmentRepository).save(argThat(DepartmentAssignment::isActif));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void assignMember_rejectsDuplicateActiveAssignment() {
        UUID memberId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Soul soul = mock(Soul.class);
        when(soul.getNomComplet()).thenReturn("Jean Dupont");
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));
        when(assignmentRepository.existsByMemberIdAndTeamIdAndActifTrue(memberId, teamId)).thenReturn(true);

        DepartmentAssignmentRequest request = new DepartmentAssignmentRequest(memberId, teamId, null,
                DepartmentAssignment.AssignmentRole.MEMBRE, null, null);

        assertThatThrownBy(() -> service.assignMember(deptId, request))
                .isInstanceOf(BusinessRuleException.class);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void endAssignment_marksInactiveAndSetsEndDate() {
        UUID assignmentId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        DepartmentAssignment assignment = DepartmentAssignment.builder()
                .id(assignmentId).departmentId(deptId).memberId(memberId).actif(true).build();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(DepartmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(soulRepository.findById(memberId)).thenReturn(Optional.empty());

        Map<String, Object> result = service.endAssignment(deptId, assignmentId);

        assertThat(result.get("actif")).isEqualTo(false);
        assertThat(assignment.getDateFin()).isEqualTo(LocalDate.now());
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    // ======================= TÂCHES =======================

    @Test
    void getTaskStats_countsOverdueOnlyForOpenTasks() {
        UUID memberId = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        DepartmentTask openLate = DepartmentTask.builder()
                .id(UUID.randomUUID()).departmentId(deptId)
                .statut(DepartmentTask.TaskStatus.EN_COURS).echeance(yesterday).assignedTo(memberId).build();
        DepartmentTask doneLate = DepartmentTask.builder()
                .id(UUID.randomUUID()).departmentId(deptId)
                .statut(DepartmentTask.TaskStatus.TERMINEE).echeance(yesterday).build();
        DepartmentTask open = DepartmentTask.builder()
                .id(UUID.randomUUID()).departmentId(deptId)
                .statut(DepartmentTask.TaskStatus.A_FAIRE).echeance(LocalDate.now().plusDays(3)).build();
        DepartmentTask blocked = DepartmentTask.builder()
                .id(UUID.randomUUID()).departmentId(deptId)
                .statut(DepartmentTask.TaskStatus.BLOQUEE).echeance(LocalDate.now().minusDays(2)).build();

        when(taskRepository.findByDepartmentIdOrderByEcheanceAsc(deptId))
                .thenReturn(List.of(openLate, doneLate, open, blocked));

        Map<String, Object> stats = service.getTaskStats(deptId);

        assertThat(stats.get("total")).isEqualTo(4L);
        assertThat(stats.get("ouvertes")).isEqualTo(3L);
        assertThat(stats.get("enRetard")).isEqualTo(2L); // openLate + blocked, doneLate exclue
        assertThat(stats.get("terminees")).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> charge = (List<Map<String, Object>>) stats.get("chargeParMembre");
        assertThat(charge).hasSize(1);
        assertThat(charge.get(0).get("memberId")).isEqualTo(memberId);
        assertThat(charge.get(0).get("tachesOuvertes")).isEqualTo(1L);
        assertThat(charge.get(0).get("enRetard")).isEqualTo(1L);
    }

    @Test
    void createTask_clampsAvancementTo100() {
        DepartmentTaskRequest request = new DepartmentTaskRequest("Montage vidéo", null, null, null,
                DepartmentTask.TaskPriority.HAUTE, DepartmentTask.TaskStatus.A_FAIRE, null, null, 150);
        when(taskRepository.save(any(DepartmentTask.class))).thenAnswer(inv -> {
            DepartmentTask t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        Map<String, Object> task = service.createTask(deptId, request);

        assertThat(task.get("avancement")).isEqualTo(100);
        assertThat(task.get("priorite")).isEqualTo("HAUTE");
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    // ======================= SÉCURITÉ / ACCÈS =======================

    @Test
    void rejectsAccessWhenDepartmentNotOwned() {
        UUID foreignDept = UUID.randomUUID();
        when(departmentService.findById(foreignDept))
                .thenThrow(new AccessDeniedException("Accès refusé"));

        assertThatThrownBy(() -> service.getTeams(foreignDept))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(teamRepository);
    }

    // ======================= MEMBRES =======================

    @Test
    void addMember_linksSoulAndRecordsActivity() {
        UUID memberId = UUID.randomUUID();
        Soul soul = mock(Soul.class);
        when(soul.getNomComplet()).thenReturn("Jean Dupont");
        when(soul.getId()).thenReturn(memberId);
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, deptId)).thenReturn(false);
        when(securityUtils.getCurrentUserRole()).thenReturn("RESPONSABLE");

        Map<String, Object> result = service.addMember(deptId, memberId);

        assertThat(result.get("id")).isEqualTo(memberId);
        assertThat(result.get("nomComplet")).isEqualTo("Jean Dupont");
        verify(soulDepartmentRepository).save(argThat(l -> l.getSoulId().equals(memberId) && l.isActif()));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void addMember_rejectsWhenAlreadyInDepartment() {
        UUID memberId = UUID.randomUUID();
        Soul soul = mock(Soul.class);
        when(soul.getNomComplet()).thenReturn("Jean Dupont");
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, deptId)).thenReturn(true);

        assertThatThrownBy(() -> service.addMember(deptId, memberId))
                .isInstanceOf(BusinessRuleException.class);
        verify(soulDepartmentRepository, never()).save(any());
    }

    @Test
    void addMember_notifiesResponsableWhenActorIsNotTheResponsable() {
        UUID memberId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();
        Soul soul = mock(Soul.class);
        when(soul.getNomComplet()).thenReturn("Jean Dupont");
        when(soul.getId()).thenReturn(memberId);
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, deptId)).thenReturn(false);
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());

        Department department = new Department();
        department.setResponsableId(responsableId);
        when(departmentService.findById(deptId)).thenReturn(department);

        service.addMember(deptId, memberId);

        verify(notificationService).create(eq(responsableId), any(), any(), any(), any(), eq(memberId), eq("SOUL"));
    }

    @Test
    void createMember_createsSoulAndLinksToDepartment() {
        var request = new com.discipolat.modules.departments.api.DepartmentCreateMemberRequest(
                "Dupont", "Jean", "jean@mail.com", "0600000000", null,
                null, "Ingénieur", null, null, null, null, null, null, null, null, null);
        UUID soulId = UUID.randomUUID();
        when(soulRepository.save(any(Soul.class))).thenAnswer(inv -> {
            Soul s = inv.getArgument(0);
            s.setId(soulId);
            return s;
        });

        Map<String, Object> result = service.createMember(deptId, request);

        assertThat(result.get("id")).isEqualTo(soulId);
        assertThat(result.get("nomComplet")).isEqualTo("Jean Dupont");
        verify(soulDepartmentRepository).save(argThat(l -> l.getSoulId().equals(soulId) && l.isActif()));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void removeMember_deactivatesLinkAndClosesAssignments() {
        UUID memberId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        Soul soul = mock(Soul.class);
        when(soul.getNomComplet()).thenReturn("Jean Dupont");
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));

        SoulDepartment link = SoulDepartment.builder()
                .soulId(memberId).departmentId(deptId).actif(true).build();
        when(soulDepartmentRepository.findBySoulIdAndDepartmentId(memberId, deptId))
                .thenReturn(List.of(link));

        DepartmentAssignment assignment = DepartmentAssignment.builder()
                .id(assignmentId).departmentId(deptId).memberId(memberId).actif(true).build();
        when(assignmentRepository.findByDepartmentIdAndMemberIdAndActifTrue(deptId, memberId))
                .thenReturn(List.of(assignment));

        service.removeMember(deptId, memberId);

        assertThat(link.isActif()).isFalse();
        assertThat(assignment.isActif()).isFalse();
        assertThat(assignment.getDateFin()).isEqualTo(LocalDate.now());
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void deleteTask_cancelsTaskInsteadOfPhysicalDelete() {
        UUID taskId = UUID.randomUUID();
        DepartmentTask task = DepartmentTask.builder()
                .id(taskId).departmentId(deptId).titre("Montage").build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(DepartmentTask.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteTask(deptId, taskId);

        assertThat(task.getStatut()).isEqualTo(DepartmentTask.TaskStatus.ANNULEE);
        verify(taskRepository, never()).delete(any());
        verify(activityRepository).save(any(DepartmentActivity.class));
    }
}
