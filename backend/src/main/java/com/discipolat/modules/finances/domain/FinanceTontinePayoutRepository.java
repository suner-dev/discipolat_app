package com.discipolat.modules.finances.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceTontinePayoutRepository extends JpaRepository<FinanceTontinePayout, UUID> {

    List<FinanceTontinePayout> findByTenantIdAndTontineIdOrderByPayoutDateDesc(UUID tenantId, UUID tontineId);
}
