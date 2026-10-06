package com.discipolat.modules.tasks.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Task> findByTenantIdAndStatus(UUID tenantId, Task.TaskStatus status, Pageable pageable);

    Page<Task> findByTenantIdAndAssignedToId(UUID tenantId, UUID assignedToId, Pageable pageable);

    Page<Task> findByTenantIdAndParentTaskId(UUID tenantId, Long parentTaskId, Pageable pageable);

    List<Task> findByTenantIdAndStatusAndDueDateBefore(UUID tenantId, Task.TaskStatus status, java.time.Instant now);

    List<Task> findByTenantIdAndDueDateBeforeAndStatusNot(UUID tenantId, java.time.Instant now, Task.TaskStatus status);

    Optional<Task> findByTenantIdAndId(UUID tenantId, Long id);

    long countByTenantIdAndStatus(UUID tenantId, Task.TaskStatus status);

    long countByTenantId(UUID tenantId);

    /**
     * Filtres combinés de {@code GET /tasks}.
     *
     * <p>Chaque critère est facultatif ({@code null} = non filtré). Le filtrage
     * s'effectue AVANT le découpage en pages, ce qui garantit une pagination
     * correcte quel que soit le nombre de filtres combinés.
     *
     * @param overdueOnly     restrict aux tâches échues et non terminées
     * @param currentUserId   « mes tâches » (null = pas de filtre)
     * @param search          recherche sur le titre ET la description
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.tenantId = :tenantId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:type IS NULL OR t.type = :type)
              AND (:assignedToId IS NULL OR t.assignedToId = :assignedToId)
              AND (:projectId IS NULL OR t.projectId = :projectId)
              AND (:departmentId IS NULL OR t.departmentId = :departmentId)
              AND (:currentUserId IS NULL OR t.assignedToId = :currentUserId)
              AND (:overdueOnly = false OR (t.dueDate IS NOT NULL
                                            AND t.dueDate < :now
                                            AND t.status <> :doneStatus))
              AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
                                 OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Task> findAll(@Param("tenantId") UUID tenantId,
                       @Param("status") Task.TaskStatus status,
                       @Param("priority") Task.TaskPriority priority,
                       @Param("type") Task.TaskType type,
                       @Param("assignedToId") UUID assignedToId,
                       @Param("projectId") Long projectId,
                       @Param("departmentId") Long departmentId,
                       @Param("search") String search,
                       @Param("overdueOnly") boolean overdueOnly,
                       @Param("currentUserId") UUID currentUserId,
                       @Param("now") Instant now,
                       @Param("doneStatus") Task.TaskStatus doneStatus,
                       Pageable pageable);
}
