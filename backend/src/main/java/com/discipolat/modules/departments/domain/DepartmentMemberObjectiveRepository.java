package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentMemberObjectiveRepository extends JpaRepository<DepartmentMemberObjective, UUID> {

    List<DepartmentMemberObjective> findByMemberIdOrderByEcheanceAscCreatedAtDesc(UUID memberId);

    List<DepartmentMemberObjective> findByMemberIdAndDepartmentIdOrderByEcheanceAscCreatedAtDesc(UUID memberId, UUID departmentId);

    List<DepartmentMemberObjective> findByDepartmentId(UUID departmentId);
}
