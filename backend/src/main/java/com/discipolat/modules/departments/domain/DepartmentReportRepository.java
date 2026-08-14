package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentReportRepository extends JpaRepository<DepartmentReport, UUID> {
    List<DepartmentReport> findByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);
    List<DepartmentReport> findByDepartmentIdAndTypeOrderByCreatedAtDesc(UUID departmentId, DepartmentReport.ReportType type);
    void deleteByDepartmentId(UUID departmentId);
}
