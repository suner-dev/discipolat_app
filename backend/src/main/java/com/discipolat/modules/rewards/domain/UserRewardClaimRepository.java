package com.discipolat.modules.rewards.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRewardClaimRepository extends JpaRepository<UserRewardClaim, Long> {
    List<UserRewardClaim> findByUserIdAndTenantIdOrderByClaimedAtDesc(Long userId, Long tenantId);
}
