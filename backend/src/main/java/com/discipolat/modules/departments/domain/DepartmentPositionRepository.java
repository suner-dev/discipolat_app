package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentPositionRepository extends JpaRepository<DepartmentPosition, UUID> {

    List<DepartmentPosition> findByDepartmentIdOrderByNomAsc(UUID departmentId);

    List<DepartmentPosition> findByDepartmentIdAndStatut(UUID departmentId, DepartmentPosition.PositionStatus statut);

    long countByDepartmentIdAndStatut(UUID departmentId, DepartmentPosition.PositionStatus statut);
}
