package com.discipolat.modules.gantt.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamTaskRepository extends JpaRepository<TeamTask, UUID> {
    List<TeamTask> findByTenantIdOrderByDateDebutAsc(UUID tenantId);
    List<TeamTask> findByTenantIdAndDepartmentId(UUID tenantId, UUID departmentId);
    List<TeamTask> findByTenantIdAndStatut(UUID tenantId, TeamTask.Statut statut);
    long countByTenantIdAndStatut(UUID tenantId, TeamTask.Statut statut);
}
