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

    /**
     * Variantes de casse d'un même code.
     *
     * <p>Renvoie une LISTE et non un {@code Optional} : le catalogue contient
     * 3 doublons de casse ({@code AUDIT}/{@code audit},
     * {@code PARALLEL_FOLLOWUPS}/{@code parallel_followups},
     * {@code SETTINGS}/{@code settings}), conséquence de seeds historiques.
     * Un {@code Optional} lèverait alors
     * {@code IncorrectResultSizeDataAccessException} — un 500 sur une simple
     * lecture. Le tri se fait dans {@link ModuleCatalogService}, qui applique
     * une règle déterministe.
     */
    List<ModuleDefinition> findByCodeIgnoreCase(String code);

    List<ModuleDefinition> findBySource(String source);

    List<ModuleDefinition> findByCategory(String category);

    List<ModuleDefinition> findByEnabledTrueOrderByCategoryAscDisplayOrderAsc();
}