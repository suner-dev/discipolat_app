package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentMemberReportRepository extends JpaRepository<DepartmentMemberReport, UUID> {

    List<DepartmentMemberReport> findByMemberIdAndDepartmentIdOrderByCreatedAtDesc(UUID memberId, UUID departmentId);

    List<DepartmentMemberReport> findByDepartmentId(UUID departmentId);
}
