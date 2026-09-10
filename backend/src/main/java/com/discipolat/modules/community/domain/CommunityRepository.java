package com.discipolat.modules.community.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {
    List<Community> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);
    List<Community> findByTenantIdAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, String category);
    List<Community> findByTenantIdAndVisibilityAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, String visibility);
    long countByTenantIdAndIsActiveTrue(UUID tenantId);
}
