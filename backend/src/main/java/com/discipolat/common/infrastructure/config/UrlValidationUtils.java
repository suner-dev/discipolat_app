package com.discipolat.common.infrastructure.config;

/**
 * Utilitaires de validation d'URL partagés entre les configurations d'IA.
 *
 * <p>Extrait d' {@link OllamaProperties} pour éviter que {@link
 * com.discipolat.common.infrastructure.config.SpeechToTextProperties} ne dépende
 * d'une configuration Ollama dont elle n'a pas la responsabilité. Aucune méthode
 * existante n'est supprimée : {@code OllamaProperties} conserve ses propres
 * méthodes statiques pour compatibilité ascendante.</p>
 */
public final class UrlValidationUtils {

    private UrlValidationUtils() {
        // utilitaire statique, non instantiable
    }

    /**
     * Retourne {@code true} si la valeur est non nulle et non blanche.
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Retourne {@code true} si la valeur commence par {@code http://} ou
     * {@code https://}.
     */
    public static boolean isHttpUrl(String value) {
        return value != null
                && (value.startsWith("http://") || value.startsWith("https://"));
    }
}
