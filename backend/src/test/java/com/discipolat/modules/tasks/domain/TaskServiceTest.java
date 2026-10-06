package com.discipolat.modules.tasks.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.tasks.api.dto.TaskDtos;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du service Tasks (V234).
 *
 * <p>Absent de la livraison initiale alors que la spécification (§3.3) les
 * exigeait. Ils verrouillent les défauts corrigés ici :
 * <ul>
 *   <li>{@code GET /tasks} n'utilisait que le filtre {@code status} : les dix
 *       autres filtres envoyés par le mobile étaient acceptés puis ignorés ;</li>
 *   <li>{@code DELETE /tasks/{id}}效应 supprimait DÉFINITIVEMENT la ligne
 *       (et ses dépendances en CASCADE), en violation de R9 ;</li>
 *   <li>{@code POST /tasks/{id}/reorder} déléguait à {@code updateTask}, qui
 *       ignore la clé {@code order} : le glisser-déposer ne persistait rien ;</li>
 *   <li>les champs {@code authorName}, {@code userName} et
 *       {@code dependsOnTaskTitle}, déclarés {@code required} par le modèle
 *       mobile, n'étaient jamais renvoyés ;</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID USER_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAttachmentRepository attachmentRepository;
    @Mock private TaskCommentRepository commentRepository;
    @Mock private TaskDependencyRepository dependencyRepository;
    @Mock private TaskTemplateRepository templateRepository;
    @Mock private TaskTemplateSubtaskRepository templateSubtaskRepository;
    @Mock private KanbanColumnRepository kanbanColumnRepository;
    @Mock private TaskTimeEntryRepository timeEntryRepository;
    @Mock private UserRepository userRepository;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, attachmentRepository, commentRepository,
                dependencyRepository, templateRepository, templateSubtaskRepository,
                kanbanColumnRepository, timeEntryRepository, userRepository);
        SecurityTestHelper.loginAs(ACTOR, "ADMIN");
        userInTenant(TENANT_A, USER_1, "Jean", "Bonheur");
    }

    private void userInTenant(UUID tenantId, UUID id, String first, String last) {
        User u = new User();
        u.setId(id);
        u.setTenantId(tenantId);
        u.setFirstName(first);
        u.setLastName(last);
        lenient().when(userRepository.findByIdWithActiveMembershipInTenant(id, tenantId))
                .thenReturn(Optional.of(u));
        lenient().when(userRepository.findById(id)).thenReturn(Optional.of(u));
    }

    private Task task(Long id, UUID tenantId, String title) {
        Task t = Task.builder()
                .id(id).tenantId(tenantId).title(title)
                .type(Task.TaskType.TASK)
                .priority(Task.TaskPriority.MEDIUM)
                .status(Task.TaskStatus.TODO)
                .build();
        invokeOnCreate(t);
        return t;
    }

    private static void invokeOnCreate(Object entity) {
        try {
            entity.getClass().getDeclaredMethod("onCreate").invoke(entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("callback @PrePersist introuvable", e);
        }
    }

    // ==================== FILTRES DE LISTE ====================

    @Nested
    @DisplayName("Filtres de GET /tasks")
    class ListFilters {

        @Test
        @DisplayName("la priorité et le type sont transmis au repository")
        void list_appliesPriorityAndType() {
            TaskDtos.TaskFilter filter = new TaskDtos.TaskFilter(
                    null, "HIGH", "BUG", null, null, null, null, null, null);
            when(taskRepository.findAll(eq(TENANT_A), eq(null), eq(Task.TaskPriority.HIGH),
                    eq(Task.TaskType.BUG), eq(null), eq(null), eq(null), eq(null),
                    eq(false), eq(null), any(), eq(Task.TaskStatus.DONE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(task(1L, TENANT_A, "Corriger le bug"))));

            List<Map<String, Object>> rows = service.listTasks(TENANT_A, filter, 0, 20);

            assertThat(rows).hasSize(1);
            verify(taskRepository).findAll(eq(TENANT_A), eq(null), eq(Task.TaskPriority.HIGH),
                    eq(Task.TaskType.BUG), eq(null), eq(null), eq(null), eq(null),
                    eq(false), eq(null), any(), eq(Task.TaskStatus.DONE), any(Pageable.class));
        }

        @Test
        @DisplayName("« mes tâches » restreint à l'utilisateur courant")
        void list_appliesMyTasks() {
            TaskDtos.TaskFilter filter = new TaskDtos.TaskFilter(
                    null, null, null, null, null, null, null, null, true);
            when(taskRepository.findAll(any(), any(), any(), any(), any(), any(), any(),
                    any(), eq(false), eq(ACTOR), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.listTasks(TENANT_A, filter, 0, 20);

            verify(taskRepository).findAll(any(), any(), any(), any(), any(), any(), any(),
                    any(), eq(false), eq(ACTOR), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("« en retard » active le filtre d'échéance")
        void list_appliesOverdue() {
            TaskDtos.TaskFilter filter = new TaskDtos.TaskFilter(
                    null, null, null, null, null, null, null, true, null);
            when(taskRepository.findAll(any(), any(), any(), any(), any(), any(), any(),
                    any(), eq(true), eq(null), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.listTasks(TENANT_A, filter, 0, 20);

            verify(taskRepository).findAll(any(), any(), any(), any(), any(), any(), any(),
                    any(), eq(true), eq(null), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("un statut inconnu est rejeté en 400")
        void list_rejectsUnknownStatus() {
            TaskDtos.TaskFilter filter = new TaskDtos.TaskFilter(
                    "FAIT", null, null, null, null, null, null, null, null);
            assertThatThrownBy(() -> service.listTasks(TENANT_A, filter, 0, 20))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("le nom camelCase du client Dart est accepté")
        void list_acceptsDartCamelCaseStatus() {
            TaskDtos.TaskFilter filter = new TaskDtos.TaskFilter(
                    "inProgress", null, null, null, null, null, null, null, null);
            when(taskRepository.findAll(any(), eq(Task.TaskStatus.IN_PROGRESS), any(), any(), any(),
                    any(), any(), any(), eq(false), eq(null), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // Le client envoie status.name du Dart : « inProgress ».
            assertThat(service.listTasks(TENANT_A, filter, 0, 20)).isEmpty();
        }

        @Test
        @DisplayName("la taille de page est bornée (R8)")
        void list_clampsPageSize() {
            when(taskRepository.findByTenantId(eq(TENANT_A), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.listTasks(TENANT_A, null, 0, 999_999);

            org.mockito.ArgumentCaptor<Pageable> captor =
                    org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(taskRepository).findByTenantId(eq(TENANT_A), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        }
    }

    // ==================== SUPPRESSION ====================

    @Nested
    @DisplayName("Suppression")
    class Deletion {

        @Test
        @DisplayName("DELETE archive la tâche (CANCELLED) au lieu de la purger — R9")
        void deleteTask_archivesInsteadOfPurging() {
            Task t = task(1L, TENANT_A, "À supprimer");
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deleteTask(TENANT_A, 1L);

            // Aucune purge, aucun CASCADE destructif.
            verify(taskRepository, never()).delete(any());
            verify(taskRepository).save(any());
            assertThat(t.getStatus()).isEqualTo(Task.TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("supprimer une tâche d'un autre tenant lève 404")
        void deleteTask_otherTenant_isNotFound() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteTask(TENANT_A, 9L))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(taskRepository, never()).delete(any());
        }
    }

    // ==================== KANBAN ====================

    @Nested
    @DisplayName("Réordonnancement Kanban")
    class Reorder {

        @Test
        @DisplayName("reorder persiste réellement le rang (régression du drag & drop)")
        void reorder_persistsOrder() {
            Task t = task(1L, TENANT_A, "Tâche");
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> view = service.reorderTask(TENANT_A, 1L, "IN_PROGRESS", 3);

            assertThat(view).containsEntry("sortOrder", 3);
            assertThat(view).containsEntry("status", "IN_PROGRESS");
            assertThat(t.getSortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("reorder vers DONE horodate la fin de tâche")
        void reorder_toDone_setsCompletionDate() {
            Task t = task(1L, TENANT_A, "Tâche");
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> view = service.reorderTask(TENANT_A, 1L, "DONE", null);

            assertThat(view).containsEntry("status", "DONE");
            assertThat(view.get("completedDate")).isNotNull();
        }

        @Test
        @DisplayName("quitter DONE efface la date de fin (cohérence du statut)")
        void reorder_leavingDone_clearsCompletionDate() {
            Task t = task(1L, TENANT_A, "Tâche");
            t.setStatus(Task.TaskStatus.DONE);
            t.setCompletedDate(Instant.parse("2026-01-01T00:00:00Z"));
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> view = service.reorderTask(TENANT_A, 1L, "TODO", 1);

            assertThat(view.get("completedDate")).isNull();
        }

        @Test
        @DisplayName("un statut inconnu est rejeté")
        void reorder_rejectsUnknownStatus() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(task(1L, TENANT_A, "Tâche")));

            assertThatThrownBy(() -> service.reorderTask(TENANT_A, 1L, "N_IMPORTE_QUOI", 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== ISOLATION (R4) ====================

    @Nested
    @DisplayName("Isolation multi-tenant")
    class TenantIsolation {

        @Test
        @DisplayName("assigner un utilisateur d'un autre tenant est refusé")
        void assign_foreignUser_isRejected() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(task(1L, TENANT_A, "Tâche")));
            UUID foreigner = UUID.fromString("99999999-9999-9999-9999-999999999999");
            when(userRepository.findByIdWithActiveMembershipInTenant(foreigner, TENANT_A))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignTask(TENANT_A, 1L, foreigner))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("lire une tâche d'un autre tenant lève 404")
        void get_otherTenant_isNotFound() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTask(TENANT_A, 5L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ==================== CONTRAT MOBILE ====================

    @Nested
    @DisplayName("Charge utile conforme au modèle mobile")
    class MobileContract {

        @Test
        @DisplayName("les commentaires exposent authorName (required côté mobile)")
        void comments_exposeAuthorName() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(task(1L, TENANT_A, "Tâche")));
            TaskComment comment = TaskComment.builder()
                    .id(1L).tenantId(TENANT_A).taskId(1L)
                    .authorId(USER_1).content("Bonjour").isSystem(false)
                    .build();
            invokeOnCreate(comment);
            when(commentRepository.findByTenantIdAndTaskIdOrderByCreatedAtAsc(TENANT_A, 1L))
                    .thenReturn(List.of(comment));

            List<Map<String, Object>> rows = service.listComments(TENANT_A, 1L);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)).containsEntry("authorName", "Jean Bonheur");
        }

        @Test
        @DisplayName("les dépendances exposent dependsOnTaskTitle (required côté mobile)")
        void dependencies_exposeTitle() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(task(1L, TENANT_A, "Tâche")));
            when(taskRepository.findByTenantIdAndId(TENANT_A, 2L))
                    .thenReturn(Optional.of(task(2L, TENANT_A, "Tâche préalable")));
            TaskDependency dep = TaskDependency.builder()
                    .id(1L).tenantId(TENANT_A).taskId(1L).dependsOnTaskId(2L)
                    .type(TaskDependency.DependencyType.BLOCKS)
                    .build();
            when(dependencyRepository.findByTenantIdAndTaskId(TENANT_A, 1L)).thenReturn(List.of(dep));

            List<Map<String, Object>> rows = service.listDependencies(TENANT_A, 1L);

            assertThat(rows.get(0)).containsEntry("dependsOnTaskTitle", "Tâche préalable");
        }

        @Test
        @DisplayName("le total de temps utilise la somme SQL et non un chargement integral")
        void timeTotal_usesAggregation() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(task(1L, TENANT_A, "Tâche")));
            when(timeEntryRepository.sumDurationMinutes(TENANT_A, 1L)).thenReturn(120);

            Map<String, Object> view = service.getTimeTotal(TENANT_A, 1L);

            assertThat(view).containsEntry("totalMinutes", 120);
            verify(timeEntryRepository).sumDurationMinutes(TENANT_A, 1L);
        }

        @Test
        @DisplayName("un total de temps nul ne renvoie pas null")
        void timeTotal_nullSum_isZero() {
            when(taskRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(task(1L, TENANT_A, "Tâche")));
            when(timeEntryRepository.sumDurationMinutes(TENANT_A, 1L)).thenReturn(null);

            assertThat(service.getTimeTotal(TENANT_A, 1L)).containsEntry("totalMinutes", 0);
        }
    }
}
