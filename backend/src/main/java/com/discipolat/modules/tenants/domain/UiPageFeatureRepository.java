package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * LOT 2 §LB — repository des fonctionnalités activables par écran.
 */
public interface UiPageFeatureRepository extends JpaRepository<UiPageFeature, UUID> {

    List<UiPageFeature> findByTenantIdIsNullOrTenantId(UUID tenantId);

    List<UiPageFeature> findByTenantId(UUID tenantId);

    List<UiPageFeature> findByTenantIdAndPageKey(UUID tenantId, String pageKey);

    void deleteByTenantIdAndPageKeyAndFeatureKey(UUID tenantId, String pageKey, String featureKey);
}