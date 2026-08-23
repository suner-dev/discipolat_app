package com.discipolat.modules.marketplace.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long> {
    List<MarketplaceListing> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(Long tenantId);
    List<MarketplaceListing> findByTenantIdAndCategoryAndIsActiveTrue(Long tenantId, String category);
}
