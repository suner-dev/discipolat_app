package com.discipolat.modules.encouragements.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EncouragementRepository extends JpaRepository<Encouragement, UUID> {
    List<Encouragement> findByToUserIdOrderByCreatedAtDesc(UUID toUserId);
    List<Encouragement> findByFromUserIdOrderByCreatedAtDesc(UUID fromUserId);
    long countByToUserId(UUID toUserId);
}
