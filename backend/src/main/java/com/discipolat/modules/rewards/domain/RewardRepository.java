package com.discipolat.modules.rewards.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByTenantIdAndIsActiveTrue(Long tenantId);
}
