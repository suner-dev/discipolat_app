package com.discipolat.common.infrastructure.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration du LLM local Ollama (M6 — « Ollama codé en dur sur localhost »).
 *
 * <p>Avant cette classe, l'URL Ollama était un {@code @Value} codé en dur sur
 * {@code http://localhost:11434} : aucun moyen de distinguer « aucune IA
 * configurée » d'une « IA cassée », et l'appel partait vers localhost même en
 * production. Cette classe rend l'état de l'IA explicite et vérifiable.</p>
 *
 * <p>Règles d'honnêteté appliquées :</p>
 * <ul>
 *   <li><b>Aucun credential n'existe ici</b> : Ollama est un LLM local, il
 *       n'exige aucune clé. Il n'y a donc rien à redactionner ni à exposer.</li>
 *   <li><b>Désactivé par défaut</b> : {@code enabled=false}. Tant que cette
 *       bascule est fausse, l'application ne doit tenter aucun appel IA —
 *       l'URL par défaut n'est qu'une commodité de développement local.</li>
 *   <li><b>Échec bruyant</b> : une URL non http(s), un modèle vide ou un timeout
 *       non positif font échouer le démarrage ({@code @Validated}) au lieu de
 *       retomber silencieusement sur un défaut.</li>
 * </ul>
 *
 * <p>Enregistrement : {@link AiConfiguration}.</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.ollama")
public class OllamaProperties {

    /** URL http(s) valide, ou chaîne vide (= « non configuré »). */
    private static final String HTTP_URL_OR_BLANK = "^$|^https?://\\S+$";

    /** URL de développement par défaut — sans effet tant que {@code enabled=false}. */
    public static final String DEFAULT_URL = "http://localhost:11434";

    /** Modèle par défaut, aligné sur {@code app.ai.model}. */
    public static final String DEFAULT_MODEL = "llama3";

    /** Timeout par défaut : un LLM lent ne doit pas bloquer un thread de requête. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /** Bascule générale : n'appeler Ollama que si elle est explicitement activée. */
    private boolean enabled = false;

    /** URL de base d'Ollama (health : {@code GET /api/tags}, chat : {@code POST /api/chat}). */
    @Pattern(regexp = HTTP_URL_OR_BLANK,
            message = "app.ollama.url doit être une URL http(s) valide (ex : http://localhost:11434) ou vide")
    private String url = DEFAULT_URL;

    /** Modèle Ollama interrogé (ex : llama3, mistral, qwen2.5). */
    @NotBlank(message = "app.ollama.model ne peut pas être vide (ex : llama3)")
    private String model = DEFAULT_MODEL;

    /** Timeout d'un appel Ollama. Un nombre seul est interprété en secondes. */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout = DEFAULT_TIMEOUT;

    /**
     * Indique si l'IA locale est réellement utilisable : bascule activée, URL
     * connue, modèle nommé et timeout exploitable.
     */
    public boolean isConfigured() {
        return enabled
                && isNotBlank(url)
                && isHttpUrl(url)
                && isNotBlank(model)
                && isPositiveTimeout();
    }

    /**
     * URL affichable : le segment {@code user:password@} éventuel est masqué
     * pour qu'aucun credential ne puisse fuiter via un log ou un toString.
     */
    public String maskedUrl() {
        if (url == null || url.isBlank()) {
            return "";
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        int at = url.indexOf('@', scheme + 3);
        if (at < 0) {
            return url;
        }
        int slash = url.indexOf('/', scheme + 3);
        if (slash >= 0 && slash < at) {
            return url;
        }
        return url.substring(0, scheme + 3) + "***" + url.substring(at);
    }

    @AssertTrue(message = "app.ollama.enabled=true exige une app.ollama.url non vide (ex : http://localhost:11434)")
    public boolean isUrlPresentWhenEnabled() {
        return !enabled || isNotBlank(url);
    }

    @AssertTrue(message = "app.ollama.timeout doit être strictement positif (ex : 30s)")
    public boolean isTimeoutStrictlyPositive() {
        return isPositiveTimeout();
    }

    private boolean isPositiveTimeout() {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    static boolean isHttpUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Aucun secret ici par construction (Ollama est local) : seul l'URL masquée
     * est exposée, jamais une clé.
     */
    @Override
    public String toString() {
        return "OllamaProperties{enabled=" + enabled
                + ", url='" + maskedUrl() + "'"
                + ", model='" + model + "'"
                + ", timeout=" + timeout + '}';
    }
}
