package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurchSettingsRepository extends JpaRepository<ChurchSettings, UUID> {

    /** La ligne de configuration est un singleton : on récupère la première créée. */
    Optional<ChurchSettings> findFirstByOrderByCreatedAtAsc();
}
