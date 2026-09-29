package com.discipolat.common.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Journalise, au démarrage et sans aucun appel réseau, ce qui manque pour que
 * l'IA locale et la dictée vocale soient utilisables.
 *
 * <p>Même principe que {@link SecurityStartupAudit} : une configuration absente
 * doit se voir dans les journaux du déploiement, pas se découvrir à la première
 * question posée par un utilisateur. Aucun secret n'est journalisé — la clé STT
 * n'apparaît que masquée.</p>
 *
 * <p>Ce composant ne contacte jamais Ollama : un serveur IA lent ou injoignable
 * ne peut pas retarder le démarrage.</p>
 */
@Component
public class AiConfigurationStartupAudit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiConfigurationStartupAudit.class);

    private final OllamaHealth ollamaHealth;
    private final SpeechToTextProperties speechProperties;

    public AiConfigurationStartupAudit(OllamaHealth ollamaHealth, SpeechToTextProperties speechProperties) {
        this.ollamaHealth = ollamaHealth;
        this.speechProperties = speechProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (ollamaHealth.isConfigured()) {
            log.info("[AI] ✅ IA locale configurée (modèle Ollama '{}'). La joignabilité du serveur "
                    + "n'est vérifiée qu'à l'appel, jamais au démarrage.", ollamaHealth.model());
        } else {
            log.warn("[AI] ⚠️ {} Les appels IA locaux répondront 503 {}. "
                            + "Les replis déterministes restent actifs et ne doivent pas être présentés comme des réponses IA.",
                    ollamaHealth.reason(), AiNotConfiguredException.CODE);
        }

        if (speechProperties.isConfigured()) {
            log.info("[STT] ✅ Dictée vocale configurée (modèle '{}', clé {}).",
                    speechProperties.getModel(), speechProperties.redactedApiKey());
        } else {
            log.info("[STT] ℹ️ {}", speechProperties.reason());
        }
    }
}
