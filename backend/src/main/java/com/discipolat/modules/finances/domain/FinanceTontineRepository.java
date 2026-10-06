package com.discipolat.modules.finances.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceTontineRepository extends JpaRepository<FinanceTontine, UUID> {

    List<FinanceTontine> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<FinanceTontine> findByTenantIdAndId(UUID tenantId, UUID id);
}
