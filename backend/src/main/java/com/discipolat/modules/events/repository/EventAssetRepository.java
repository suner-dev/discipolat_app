package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventAssetRepository extends JpaRepository<EventAsset, UUID> {

    List<EventAsset> findByChurchEventId(UUID churchEventId);

    List<EventAsset> findByAssetId(UUID assetId);
}