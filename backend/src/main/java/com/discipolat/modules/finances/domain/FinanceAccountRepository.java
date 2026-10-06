package com.discipolat.modules.finances.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, UUID> {

    List<FinanceAccount> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<FinanceAccount> findByTenantIdAndId(UUID tenantId, UUID id);
}
