package com.discipolat.modules.evangelism.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvangelismTrackRepository extends JpaRepository<EvangelismTrack, UUID> {
    Optional<EvangelismTrack> findBySoulId(UUID soulId);
    List<EvangelismTrack> findByEtapeOrderByDateEtapeDesc(EvangelismEtape etape);
    long countByEtape(EvangelismEtape etape);
    List<EvangelismTrack> findBySoulIdIn(List<UUID> soulIds);
    long countByEtapeAndDateEtapeBetween(EvangelismEtape etape, java.time.LocalDate from, java.time.LocalDate to);
    long countByEtapeAndSoulIdInAndDateEtapeBetween(EvangelismEtape etape, List<UUID> soulIds, java.time.LocalDate from, java.time.LocalDate to);
}
