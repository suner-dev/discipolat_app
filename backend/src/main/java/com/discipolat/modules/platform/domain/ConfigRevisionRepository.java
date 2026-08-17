package com.discipolat.modules.platform.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConfigRevisionRepository extends JpaRepository<ConfigRevision, UUID> {

    /**
     * Liste paginée des révisions, filtrées optionnellement par type d'entité.
     * Tri décroissant par date de création (le plus récent d'abord).
     */
    @Query("SELECT r FROM ConfigRevision r WHERE (:entityType IS NULL OR r.entityType = :entityType)")
    Page<ConfigRevision> findFiltered(@Param("entityType") String entityType, Pageable pageable);
}
