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

    // ==================================================================
    // Résolution INSENSIBLE à la casse — correctif du blocage d'onboarding
    // ==================================================================
    //
    // Constat : l'étape « Modules » de l'onboarding normalisait le code en
    // minuscule puis cherchait par égalité stricte, alors que le catalogue
    // stocke majoritairement des MAJUSCULES (`SOULS`, `DASHBOARD`…). Résultat
    // reproduit : `400 STEP_DATA_INVALID — Module inconnu : souls` avec le code
    // canonique `SOULS`, donc l'étape 6/7 était impossible à valider et le
    // wizard s'arrêtait à 71 %.
    //
    // Le correctif est placé ICI, dans l'unique point de résolution, plutôt
    // qu'à l'appelant : tous les consommateurs (onboarding, ModuleRouter,
    // contrôleur du catalogue) en bénéficient, et aucun ne peut réintroduire
    // le bug en normalisant differently.

    /**
     * Résout un code de module quelle que soit sa casse.
     *
     * <p>En cas de doublons de casse, le gagnant est <b>déterministe</b> :
     * variante en majuscules d'abord (c'est la convention dominante du
     * catalogue), puis ordre alphabétique. Deux appels avec la même casse
     * renvoient donc toujours le même module — indispensable pour que la
     * validation d'onboarding et l'affichage ne puissent pas diverger.
     */
    @Transactional(readOnly = true)
    public Optional<ModuleDefinition> findByCodeAnyCase(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String trimmed = code.trim();
        // Chemin rapide et non ambigu : le code existe tel quel.
        Optional<ModuleDefinition> exact = definitionRepository.findByCode(trimmed);
        if (exact.isPresent()) {
            return exact;
        }
        List<ModuleDefinition> variants = definitionRepository.findByCodeIgnoreCase(trimmed);
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        return variants.stream().min(CANONICAL_PREFERENCE);
    }

    /**
     * Code <b>canonique</b> d'un module, à utiliser pour PERSISTER.
     *
     * <p>Point qui compte : la validation seule ne suffisait pas. Si l'on
     * validait `souls` contre le module `SOULS` mais qu'on persistait `souls`
     * dans {@code tenant_features.module_code}, les autres résolutions
     * (strictes) ne retrouveraient plus jamais ce module — activé mais
     * invisible. On renvoie donc explicitement le code canonique du catalogue.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolveCanonicalCode(String code) {
        return findByCodeAnyCase(code).map(ModuleDefinition::getCode);
    }

    /**
     * Variantes d'un même code, sans doublon logique : une entrée par module.
     *
     * <p>Utilisé pour l'affichage du catalogue, afin qu'un module seedé deux
     * fois ({@code AUDIT} et {@code audit}) ne s'affiche pas deux fois dans un
     * sélecteur. Aucune ligne n'est supprimée — c'est une règle de présentation.
     */
    @Transactional(readOnly = true)
    public List<ModuleDefinition> getDeduplicatedEnabledDefinitions() {
        Map<String, ModuleDefinition> best = new LinkedHashMap<>();
        for (ModuleDefinition definition
                : definitionRepository.findByEnabledTrueOrderByCategoryAscDisplayOrderAsc()) {
            String canonicalKey = definition.getCode().trim().toUpperCase(Locale.ROOT);
            ModuleDefinition current = best.get(canonicalKey);
            if (current == null || CANONICAL_PREFERENCE.compare(definition, current) < 0) {
                best.put(canonicalKey, definition);
            }
        }
        return new ArrayList<>(best.values());
    }

    /** Majuscules d'abord (convention du catalogue), puis alphabétique. */
    private static final Comparator<ModuleDefinition> CANONICAL_PREFERENCE =
            Comparator.comparingInt((ModuleDefinition d) -> isUpperCaseCode(d.getCode()) ? 0 : 1)
                    .thenComparing(ModuleDefinition::getCode);

    private static boolean isUpperCaseCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return code.equals(code.toUpperCase(Locale.ROOT));
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