package com.discipolat.modules.souls.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoulDepartmentRepository extends JpaRepository<SoulDepartment, SoulDepartmentId> {

    List<SoulDepartment> findBySoulIdAndActifTrue(UUID soulId);

    List<SoulDepartment> findByDepartmentIdAndActifTrue(UUID departmentId);

    List<SoulDepartment> findBySoulId(UUID soulId);

    List<SoulDepartment> findByDepartmentId(UUID departmentId);

    List<SoulDepartment> findByDepartmentIdIn(List<UUID> departmentIds);

    long countByDepartmentIdAndActifTrue(UUID departmentId);

    boolean existsBySoulIdAndDepartmentIdAndActifTrue(UUID soulId, UUID departmentId);
}
