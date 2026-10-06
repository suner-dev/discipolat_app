package com.discipolat.modules.health.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignParticipantRepository extends JpaRepository<CampaignParticipant, UUID> {

    List<CampaignParticipant> findByTenantIdAndCampaignId(UUID tenantId, UUID campaignId);

    Optional<CampaignParticipant> findByTenantIdAndId(UUID tenantId, UUID id);
}
