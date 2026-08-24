package com.discipolat.modules.spiritualChallenges.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpiritualChallengeRepository extends JpaRepository<SpiritualChallenge, UUID> {
    Page<SpiritualChallenge> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<SpiritualChallenge> findByTenantIdAndAssignéÀ(UUID tenantId, UUID assignéÀ);
    long countByTenantIdAndStatut(UUID tenantId, SpiritualChallenge.Statut statut);
}
