package com.discipolat.modules.community.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, UUID> {
    List<CommunityPost> findByTenantIdAndCommunityIdAndIsActiveTrueOrderByIsPinnedDescCreatedAtDesc(UUID tenantId, UUID communityId);
    List<CommunityPost> findByTenantIdAndAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, UUID authorId);
    List<CommunityPost> findByTenantIdAndPostTypeAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId, String postType);
    long countByTenantIdAndCommunityIdAndIsActiveTrue(UUID tenantId, UUID communityId);
}
