package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * G2.2 — Repository pour le catalogue global des modules
 */
@Repository
public interface ModuleDefinitionRepository extends JpaRepository<ModuleDefinition, UUID> {

    Optional<ModuleDefinition> findByCode(String code);

    List<ModuleDefinition> findBySource(String source);

    List<ModuleDefinition> findByCategory(String category);

    List<ModuleDefinition> findByEnabledTrueOrderByCategoryAscDisplayOrderAsc();
}