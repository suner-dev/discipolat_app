package com.discipolat.modules.souls.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpiritualScoreRepository extends JpaRepository<SpiritualScore, UUID> {
    Optional<SpiritualScore> findBySoulIdAndSemaine(UUID soulId, LocalDate semaine);
    List<SpiritualScore> findBySoulIdOrderBySemaineAsc(UUID soulId);
    void deleteBySoulIdAndSemaine(UUID soulId, LocalDate semaine);
}
