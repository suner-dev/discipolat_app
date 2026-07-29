package com.discipolat.modules.souls.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoulHistoryRepository extends JpaRepository<SoulHistory, UUID> {
    List<SoulHistory> findByAmeIdOrderByCreatedAtDesc(UUID ameId);
}
