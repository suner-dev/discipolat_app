package com.discipolat.modules.users.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, UserDepartmentId> {

    List<UserDepartment> findByUserId(UUID userId);

    List<UserDepartment> findByDepartmentId(UUID departmentId);

    List<UserDepartment> findByUserIdAndRoleDansDept(UUID userId, String roleDansDept);

    long countByDepartmentId(UUID departmentId);

    boolean existsByUserIdAndDepartmentId(UUID userId, UUID departmentId);
}
