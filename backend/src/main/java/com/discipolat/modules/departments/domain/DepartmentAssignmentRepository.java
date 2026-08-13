package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentAssignmentRepository extends JpaRepository<DepartmentAssignment, UUID> {

    List<DepartmentAssignment> findByDepartmentId(UUID departmentId);

    List<DepartmentAssignment> findByDepartmentIdAndActifTrue(UUID departmentId);

    List<DepartmentAssignment> findByTeamId(UUID teamId);

    boolean existsByMemberIdAndTeamIdAndActifTrue(UUID memberId, UUID teamId);

    List<DepartmentAssignment> findByDepartmentIdAndMemberIdAndActifTrue(UUID departmentId, UUID memberId);

    List<DepartmentAssignment> findByDepartmentIdAndMemberId(UUID departmentId, UUID memberId);
}
