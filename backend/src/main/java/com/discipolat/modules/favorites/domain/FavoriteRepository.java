package com.discipolat.modules.favorites.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByUserIdAndEntityTypeOrderByCreatedAtDesc(UUID userId, FavoriteEntityType entityType);
    Optional<Favorite> findByUserIdAndEntityTypeAndEntityId(UUID userId, FavoriteEntityType entityType, UUID entityId);
    boolean existsByUserIdAndEntityTypeAndEntityId(UUID userId, FavoriteEntityType entityType, UUID entityId);
    long countByUserIdAndEntityType(UUID userId, FavoriteEntityType entityType);
}
