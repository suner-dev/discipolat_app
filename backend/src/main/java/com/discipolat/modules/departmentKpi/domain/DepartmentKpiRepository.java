package com.discipolat.modules.departmentKpi.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentKpiRepository extends JpaRepository<DepartmentKpi, Long> {
    List<DepartmentKpi> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);
    List<DepartmentKpi> findByTenantId(Long tenantId);
}
