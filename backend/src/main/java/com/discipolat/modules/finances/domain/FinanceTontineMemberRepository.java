package com.discipolat.modules.finances.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceTontineMemberRepository extends JpaRepository<FinanceTontineMember, UUID> {

    List<FinanceTontineMember> findByTenantIdAndTontineIdOrderByTurnOrderAsc(UUID tenantId, UUID tontineId);
}
