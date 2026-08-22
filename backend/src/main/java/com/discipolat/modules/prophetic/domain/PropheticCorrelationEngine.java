package com.discipolat.modules.prophetic.domain;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de corrélation prophétique.
 * Trouve les entrées liées par tags, thèmes et mots-clés.
 */
@Component
public class PropheticCorrelationEngine {

    private static final Set<String> STOP_WORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "et", "ou", "de", "du", "au",
            "je", "tu", "il", "elle", "nous", "vous", "ils", "elles",
            "est", "sont", "a", "ont", "fait", "être", "avoir", "cette", "ce",
            "dans", "pour", "avec", "sur", "par", "pas", "plus", "aussi",
            "the", "an", "is", "are", "was", "were", "have", "has", "had"
    );

    /**
     * Calcule un score de similarité entre deux entrées prophétiques.
     * Score de 0.0 (aucune similarité) à 1.0 (identique).
     */
    public double computeSimilarity(PropheticEntry a, PropheticEntry b) {
        Set<String> tagsA = parseTags(a.getTags());
        Set<String> tagsB = parseTags(b.getTags());

        // Jaccard similarity for tags
        double tagSimilarity = 0;
        if (!tagsA.isEmpty() && !tagsB.isEmpty()) {
            Set<String> intersection = new HashSet<>(tagsA);
            intersection.retainAll(tagsB);
            Set<String> union = new HashSet<>(tagsA);
            union.addAll(tagsB);
            tagSimilarity = (double) intersection.size() / union.size();
        }

        // Content keyword similarity
        Set<String> wordsA = extractKeywords(a.getTitle() + " " + a.getContent());
        Set<String> wordsB = extractKeywords(b.getTitle() + " " + b.getContent());
        double contentSimilarity = 0;
        if (!wordsA.isEmpty() && !wordsB.isEmpty()) {
            Set<String> intersection = new HashSet<>(wordsA);
            intersection.retainAll(wordsB);
            Set<String> union = new HashSet<>(wordsA);
            union.addAll(wordsB);
            contentSimilarity = (double) intersection.size() / union.size();
        }

        // Same type bonus
        double typeBonus = a.getType() == b.getType() ? 0.1 : 0;

        return Math.min(1.0, (tagSimilarity * 0.5) + (contentSimilarity * 0.4) + typeBonus);
    }

    /**
     * Trouve les N entrées les plus corrélées à une entrée donnée.
     */
    public List<PropheticEntry> findCorrelated(PropheticEntry source,
                                                List<PropheticEntry> candidates,
                                                int maxResults) {
        return candidates.stream()
                .filter(e -> !e.getId().equals(source.getId()))
                .map(e -> Map.entry(e, computeSimilarity(source, e)))
                .filter(entry -> entry.getValue() > 0.05)
                .sorted(Map.Entry.<PropheticEntry, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) return Set.of();
        return Set.of(tags.split(",")).stream()
                .map(String::trim).map(String::toLowerCase)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> extractKeywords(String text) {
        if (text == null) return Set.of();
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }
}
