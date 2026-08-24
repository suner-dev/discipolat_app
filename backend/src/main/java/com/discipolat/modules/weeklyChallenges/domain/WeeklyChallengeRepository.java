package com.discipolat.modules.weeklyChallenges.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WeeklyChallengeRepository extends JpaRepository<WeeklyChallenge, UUID> {
    List<WeeklyChallenge> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, WeeklyChallenge.Status status);
    List<WeeklyChallenge> findByTenantIdAndWeekNumberAndYearOrderByCreatedAtDesc(UUID tenantId, Integer week, Integer year);
    long countByTenantIdAndStatus(UUID tenantId, WeeklyChallenge.Status status);
    long countByTenantIdAndAssignedToIdAndStatus(UUID tenantId, UUID userId, WeeklyChallenge.Status status);
}
