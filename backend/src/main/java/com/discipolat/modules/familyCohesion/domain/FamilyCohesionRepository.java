package com.discipolat.modules.familyCohesion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyCohesionRepository extends JpaRepository<FamilyCohesion, UUID> {
    Optional<FamilyCohesion> findByFamilleIdOrderByCalculéLeDesc(UUID familleId);
}
