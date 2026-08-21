package com.discipolat.modules.prophetic.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Journal Prophétique — espace dédié aux visions, songes et prophéties.
 *
 * Fonctionnalités :
 * - CRUD complet avec tags et portée (scope)
 * - Corrélation IA : détecte les thèmes similaires entre entrées
 * - Moteur de recherche par tags et mots-clés
 * - Statistiques par type et thème
 */
@Service
@Transactional
public class PropheticJournalService {

    private static final Logger log = LoggerFactory.getLogger(PropheticJournalService.class);

    private final PropheticEntryRepository repository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public PropheticJournalService(PropheticEntryRepository repository,
                                   EntityPropagationPublisher propagationPublisher,
                                   SecurityUtils securityUtils) {
        this.repository = repository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    public PropheticEntry create(PropheticEntry entry) {
        entry.setAuthorId(securityUtils.getCurrentUserId());
        PropheticEntry saved = repository.save(entry);
        propagationPublisher.publishCreated("PROPHETIC_ENTRY", saved.getId(),
                Map.of("type", saved.getType().name(), "title", saved.getTitle()),
                "Entrée prophétique créée: " + saved.getTitle());
        return saved;
    }

    public PropheticEntry update(UUID id, PropheticEntry updated) {
        PropheticEntry existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setTags(updated.getTags());
        existing.setScope(updated.getScope());
        existing.setPublic(updated.isPublic());
        PropheticEntry saved = repository.save(existing);
        propagationPublisher.publishUpdated("PROPHETIC_ENTRY", saved.getId(),
                Map.of(), Map.of("title", saved.getTitle()),
                "Entrée prophétique mise à jour");
        return saved;
    }

    public void delete(UUID id) {
        PropheticEntry entry = findById(id);
        propagationPublisher.publishDeleted("PROPHETIC_ENTRY", id,
                Map.of("title", entry.getTitle()),
                "Entrée prophétique supprimée");
        repository.delete(entry);
    }

    @Transactional(readOnly = true)
    public PropheticEntry findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PropheticEntry", id));
    }

    @Transactional(readOnly = true)
    public List<PropheticEntry> findByAuthor(UUID authorId) {
        return repository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    @Transactional(readOnly = true)
    public List<PropheticEntry> findByType(PropheticEntry.EntryType type) {
        return repository.findByTypeOrderByCreatedAtDesc(type);
    }

    @Transactional(readOnly = true)
    public List<PropheticEntry> findPublic() {
        return repository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PropheticEntry> findByTag(String tag) {
        return repository.findByTagsContainingIgnoreCaseOrderByCreatedAtDesc(tag);
    }

    /**
     * Corrélation IA basique : trouve les entrées avec des tags ou mots-clés similaires.
     * Version simplifiée — une version avancée utiliserait un LLM pour la corrélation sémantique.
     */
    @Transactional(readOnly = true)
    public List<PropheticEntry> findCorrelated(UUID entryId) {
        PropheticEntry entry = findById(entryId);
        Set<String> entryTags = parseTags(entry.getTags());
        if (entryTags.isEmpty()) {
            return repository.findByTypeOrderByCreatedAtDesc(entry.getType()).stream()
                    .filter(e -> !e.getId().equals(entryId))
                    .limit(5)
                    .toList();
        }
        return repository.findAll().stream()
                .filter(e -> !e.getId().equals(entryId))
                .filter(e -> {
                    Set<String> otherTags = parseTags(e.getTags());
                    return !Collections.disjoint(entryTags, otherTags);
                })
                .sorted(Comparator.comparing(PropheticEntry::getCreatedAt).reversed())
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repository.count());
        Map<String, Long> byType = repository.findAll().stream()
                .collect(Collectors.groupingBy(e -> e.getType().name(), Collectors.counting()));
        stats.put("byType", byType);
        return stats;
    }

    public UUID getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return Set.of();
        return Set.of(tags.split(",")).stream()
                .map(String::trim).map(String::toLowerCase)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }
}
