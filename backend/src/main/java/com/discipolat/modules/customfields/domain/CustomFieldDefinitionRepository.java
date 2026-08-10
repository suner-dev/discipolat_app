package com.discipolat.modules.customfields.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {
    List<CustomFieldDefinition> findByEntiteTypeAndActifTrueOrderByOrdreAsc(String entiteType);
    List<CustomFieldDefinition> findByEntiteTypeOrderByOrdreAsc(String entiteType);
}