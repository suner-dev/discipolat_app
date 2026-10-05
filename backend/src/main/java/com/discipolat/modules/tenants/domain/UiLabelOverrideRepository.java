package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * LOT 2 §LB — repository des surcharges de libellés (globales + église).
 */
public interface UiLabelOverrideRepository extends JpaRepository<UiLabelOverride, UUID> {

    List<UiLabelOverride> findByTenantIdIsNullOrTenantId(UUID tenantId);

    List<UiLabelOverride> findByTenantId(UUID tenantId);

    List<UiLabelOverride> findByTenantIdAndLabelKey(UUID tenantId, String labelKey);

    void deleteByTenantIdAndLabelKeyAndLocale(UUID tenantId, String labelKey, String locale);
}