package com.discipolat.modules.prophetic.domain;

import java.util.List;

/**
 * Statut du service Speech-to-Text.
 *
 * @param configuredProviders liste des providers configurés (null si aucun)
 * @param providers           liste complète des providers disponibles
 */
public record SpeechToTextStatus(String configuredProviders, List<String> providers) {
}
