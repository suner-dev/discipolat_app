package com.discipolat.modules.badges.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    List<UserBadge> findByUserId(UUID userId);
    Optional<UserBadge> findByUserIdAndBadgeId(UUID userId, UUID badgeId);
    long countByUserId(UUID userId);
}
