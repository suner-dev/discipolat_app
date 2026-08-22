package com.discipolat.modules.tontine.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TontineContributionRepository extends JpaRepository<TontineContribution, UUID> {
    List<TontineContribution> findByGroupIdAndTourOrderByMemberIdAsc(UUID groupId, int tour);
    Optional<TontineContribution> findByGroupIdAndMemberIdAndTour(UUID groupId, UUID memberId, int tour);
    long countByGroupIdAndPayeTrue(UUID groupId);
}
