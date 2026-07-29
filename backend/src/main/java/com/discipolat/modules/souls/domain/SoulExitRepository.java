package com.discipolat.modules.souls.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SoulExitRepository extends JpaRepository<SoulExit, UUID> {
    Optional<SoulExit> findTopByAmeIdOrderByCreatedAtDesc(UUID ameId);
    List<SoulExit> findByAmeIdOrderByCreatedAtDesc(UUID ameId);
    List<SoulExit> findByFaiseurIdOrderByCreatedAtDesc(UUID faiseurId);
    boolean existsByAmeIdAndPeutReintegrerTrue(UUID ameId);
}
