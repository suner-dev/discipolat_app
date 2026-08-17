package com.discipolat.modules.departments.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentTaskRepository extends JpaRepository<DepartmentTask, UUID> {

    List<DepartmentTask> findByDepartmentIdOrderByEcheanceAsc(UUID departmentId);

    List<DepartmentTask> findByDepartmentIdAndStatut(UUID departmentId, DepartmentTask.TaskStatus statut);

    List<DepartmentTask> findByDepartmentIdAndTeamId(UUID departmentId, UUID teamId);

    List<DepartmentTask> findByDepartmentIdAndAssignedTo(UUID departmentId, UUID assignedTo);

    List<DepartmentTask> findByDepartmentIdAndStatutIn(UUID departmentId, List<DepartmentTask.TaskStatus> statuses);

    long countByDepartmentIdAndStatut(UUID departmentId, DepartmentTask.TaskStatus statut);

    List<DepartmentTask> findByStatutInAndEcheanceBefore(List<DepartmentTask.TaskStatus> statuses, java.time.LocalDate date);

    /** Source du Page Builder TÂCHES : tâches ouvertes, plus proche échéance d'abord. */
    List<DepartmentTask> findTop10ByStatutInOrderByEcheanceAsc(List<DepartmentTask.TaskStatus> statuses);

    /** Source du Page Builder TÂCHES scopée : tâches ouvertes des départements accessibles. */
    List<DepartmentTask> findTop10ByStatutInAndDepartmentIdInOrderByEcheanceAsc(
            List<DepartmentTask.TaskStatus> statuses, Collection<UUID> departmentIds);
}
