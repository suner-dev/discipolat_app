package com.discipolat.common.infrastructure.config;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verdict sur l'utilisabilité réelle de l'IA locale (Ollama).
 *
 * <p>Sépare deux questions que la configuration confondait : « qu'est-ce qui est
 * déclaré ? » ({@link OllamaProperties}) et « l'IA est-elle utilisable, et sinon
 * pourquoi ? » (ce composant).</p>
 *
 * <p>Garanties :</p>
 * <ul>
 *   <li><b>Jamais de faux positif</b> : {@code enabled=false} ⇒
 *       {@link #isConfigured()} renvoie {@code false}, quelle que soit l'URL.
 *       C'est précisément le défaut M6 (localhost codé en dur présenté comme une
 *       IA disponible).</li>
 *   <li><b>Motif explicite</b> : {@link #reason()} nomme la clé de
 *       configuration fautive, jamais un message générique.</li>
 *   <li><b>Zéro appel réseau</b> : ce composant ne détient aucun client HTTP.
 *       Un serveur IA lent ou injoignable ne peut donc pas retarder le
 *       démarrage ni une requête.</li>
 * </ul>
 */
@Component
public class OllamaHealth {

    private final OllamaProperties properties;

    public OllamaHealth(OllamaProperties properties) {
        this.properties = properties;
    }

    /**
     * @return {@code true} seulement si l'IA locale est activée <b>et</b>
     *         complètement paramétrée
     */
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /**
     * Explication précise de l'indisponibilité, dans l'ordre des causes les
     * plus probables ; chaîne vide si l'IA locale est utilisable.
     */
    public String reason() {
        if (!properties.isEnabled()) {
            return "IA locale (Ollama) désactivée : app.ollama.enabled=false. "
                    + "Aucun appel n'est effectué et aucun repli automatique ne doit être présenté comme une réponse IA.";
        }
        String url = properties.getUrl();
        if (!OllamaProperties.isNotBlank(url)) {
            return "IA locale (Ollama) activée mais app.ollama.url est vide : "
                    + "l'adresse du serveur Ollama est inconnue.";
        }
        if (!OllamaProperties.isHttpUrl(url)) {
            return "IA locale (Ollama) activée mais app.ollama.url n'est pas une URL http(s) valide (ex : http://localhost:11434).";
        }
        if (!OllamaProperties.isNotBlank(properties.getModel())) {
            return "IA locale (Ollama) activée mais app.ollama.model est vide : aucun modèle à interroger.";
        }
        if (!isTimeoutUsable()) {
            return "IA locale (Ollama) activée mais app.ollama.timeout doit être strictement positif (ex : 30s) : "
                    + "sans timeout borné, une requête peut rester bloquée indéfiniment.";
        }
        return "";
    }

    /**
     * Garde-fou fail-closed du chemin IA. À appeler en tête de toute méthode qui
     * interroge réellement Ollama : l'appelant reçoit alors un
     * {@code 503 AI_NOT_CONFIGURED} explicite au lieu d'une erreur de
     * connexion, d'un 500, ou d'un repli silencieux.
     *
     * @throws AiNotConfiguredException si l'IA locale n'est pas utilisable
     */
    public void requireConfigured() {
        if (!isConfigured()) {
            throw new AiNotConfiguredException(reason());
        }
    }

    /**
     * Modèle Ollama configuré, à usage de journalisation uniquement.
     */
    public String model() {
        return properties.getModel();
    }

    /**
     * Instantané non sensible destiné aux endpoints de diagnostic : aucune clé
     * (Ollama n'en exige pas) et URL masquée si elle contenait un
     * {@code user:password@}.
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", properties.isEnabled());
        status.put("configured", isConfigured());
        status.put("url", properties.maskedUrl());
        status.put("model", properties.getModel());
        status.put("timeout", String.valueOf(properties.getTimeout()));
        status.put("reason", reason());
        return status;
    }

    private boolean isTimeoutUsable() {
        java.time.Duration timeout = properties.getTimeout();
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    @Override
    public String toString() {
        return "OllamaHealth{configured=" + isConfigured()
                + ", reason='" + reason() + "'}";
    }
}
