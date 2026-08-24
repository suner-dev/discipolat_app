package com.discipolat.modules.discipleshipPath.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DiscipleshipPathRepository extends JpaRepository<DiscipleshipPath, UUID> {
    List<DiscipleshipPath> findByTenantIdAndMemberId(UUID tenantId, UUID memberId);
    List<DiscipleshipPath> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, DiscipleshipPath.Status status);
    List<DiscipleshipPath> findByTenantIdAndCurrentStageOrderByCreatedAtDesc(UUID tenantId, DiscipleshipPath.Stage stage);
}
