package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformFeatureFlagRepository extends JpaRepository<PlatformFeatureFlag, UUID> {
    Optional<PlatformFeatureFlag> findByKey(String key);
    List<PlatformFeatureFlag> findByCategory(String category);
    List<PlatformFeatureFlag> findAllByOrderByCategoryAscKeyAsc();
}