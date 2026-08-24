package com.discipolat.modules.currency.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrencyConfigRepository extends JpaRepository<CurrencyConfig, UUID> {
    List<CurrencyConfig> findByTenantIdAndIsActiveTrueOrderByIsPrimaryDesc(UUID tenantId);
    Optional<CurrencyConfig> findByTenantIdAndIsPrimaryTrue(UUID tenantId);
    Optional<CurrencyConfig> findByTenantIdAndCurrencyCode(UUID tenantId, String currencyCode);
}
