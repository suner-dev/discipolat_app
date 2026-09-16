package com.discipolat.modules.tenants.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * G2.2 — Service pour le catalogue de modules (ModuleDefinition)
 * Gère le catalogue global et la déclaration des modules EXISTING
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleCatalogService {

    private final ModuleDefinitionRepository definitionRepository;

    public List<ModuleDefinition> getAllDefinitions() {
        return definitionRepository.findAll();
    }

    public List<ModuleDefinition> getEnabledDefinitions() {
        return definitionRepository.findByEnabledTrueOrderByCategoryAscDisplayOrderAsc();
    }

    public Optional<ModuleDefinition> getByCode(String code) {
        return definitionRepository.findByCode(code);
    }

    public List<ModuleDefinition> getBySource(String source) {
        return definitionRepository.findBySource(source);
    }

    public List<ModuleDefinition> getByCategory(String category) {
        return definitionRepository.findByCategory(category);
    }

    public Map<String, List<ModuleDefinition>> getGroupedByCategory() {
        return getEnabledDefinitions().stream()
                .collect(Collectors.groupingBy(ModuleDefinition::getCategory));
    }

    public long countBySource(String source) {
        return getBySource(source).size();
    }

    public long totalCount() {
        return definitionRepository.count();
    }
}