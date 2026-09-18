package com.discipolat.modules.customfields.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {
    List<CustomFieldDefinition> findByEntiteTypeAndActifTrueOrderByOrdreAsc(String entiteType);
    List<CustomFieldDefinition> findByEntiteTypeOrderByOrdreAsc(String entiteType);

    /** G4.5 — Clé d'idempotence de l'import canonique : (tenant, entité, code). */
    Optional<CustomFieldDefinition> findByTenantIdAndEntiteTypeAndCode(UUID tenantId, String entiteType, String code);
}