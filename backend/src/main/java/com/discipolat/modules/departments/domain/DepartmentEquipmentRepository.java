package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentEquipmentRepository extends JpaRepository<DepartmentEquipment, UUID> {
    List<DepartmentEquipment> findByDepartmentIdOrderByNomAsc(UUID departmentId);
    Optional<DepartmentEquipment> findByIdAndDepartmentId(UUID id, UUID departmentId);
    long countByDepartmentId(UUID departmentId);
    void deleteByDepartmentId(UUID departmentId);
}
