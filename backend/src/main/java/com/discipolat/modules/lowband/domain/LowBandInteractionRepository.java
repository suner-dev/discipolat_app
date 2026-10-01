package com.discipolat.modules.lowband.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LowBandInteractionRepository extends JpaRepository<LowBandInteraction, UUID> {
    Page<LowBandInteraction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
