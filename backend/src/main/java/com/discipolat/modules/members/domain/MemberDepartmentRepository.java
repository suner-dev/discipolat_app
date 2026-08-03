package com.discipolat.modules.members.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberDepartmentRepository extends JpaRepository<MemberDepartment, UUID> {
    List<MemberDepartment> findBySoulId(UUID soulId);
    boolean existsBySoulIdAndDepartmentId(UUID soulId, UUID departmentId);
}
