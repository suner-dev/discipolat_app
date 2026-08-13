package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentActivityRepository extends JpaRepository<DepartmentActivity, UUID> {

    List<DepartmentActivity> findTop50ByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);

    long countByDepartmentId(UUID departmentId);
}
