package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentChecklistRepository extends JpaRepository<DepartmentChecklist, UUID> {
    List<DepartmentChecklist> findByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);
    List<DepartmentChecklist> findByDepartmentIdAndCibleTypeOrderByCreatedAtDesc(UUID departmentId, DepartmentChecklist.CibleType cibleType);
    List<DepartmentChecklist> findByCibleTypeAndCibleIdOrderByCreatedAtDesc(DepartmentChecklist.CibleType cibleType, UUID cibleId);
    Optional<DepartmentChecklist> findByIdAndDepartmentId(UUID id, UUID departmentId);
    void deleteByDepartmentId(UUID departmentId);
}
