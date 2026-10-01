package com.discipolat.modules.finances.domain;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface FinanceBankStatementLineRepository extends JpaRepository<FinanceBankStatementLine, UUID> {

    List<FinanceBankStatementLine> findByTenantIdAndStatus(UUID tenantId, FinanceBankStatementLine.Status status);

    List<FinanceBankStatementLine> findByTenantIdOrderByDateTransactionDesc(UUID tenantId);

    List<FinanceBankStatementLine> findByTenantIdAndExternalKeyIn(UUID tenantId, Set<String> externalKeys);

    long countByTenantIdAndStatus(UUID tenantId, FinanceBankStatementLine.Status status);
}
