package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentTeamRepository extends JpaRepository<DepartmentTeam, UUID> {

    List<DepartmentTeam> findByDepartmentIdOrderByNomAsc(UUID departmentId);

    List<DepartmentTeam> findByDepartmentIdAndStatut(UUID departmentId, DepartmentTeam.TeamStatus statut);

    List<DepartmentTeam> findByParentId(UUID parentId);

    long countByDepartmentIdAndStatut(UUID departmentId, DepartmentTeam.TeamStatus statut);
}
