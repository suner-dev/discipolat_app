package com.discipolat.modules.referrals.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    Page<Referral> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<Referral> findByParrainId(UUID parrainId);
    long countByParrainIdAndStatut(UUID parrainId, Referral.Statut statut);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStatut(UUID tenantId, Referral.Statut statut);
}
