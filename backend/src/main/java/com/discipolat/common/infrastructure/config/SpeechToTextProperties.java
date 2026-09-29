package com.discipolat.common.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * Configuration Speech-to-Text / Whisper (M5 — « config STT documentée mais absente »).
 *
 * <p>{@code WhisperSpeechToTextProvider} lit déjà {@code app.speech.api-url},
 * {@code app.speech.api-key} et {@code app.speech.model}, mais aucune de ces
 * clés n'existait dans {@code application.yml} : la feature était donc
 * impossible à configurer sans recompiler, alors que la documentation la
 * présentait comme fonctionnelle. Ce bloc rend la configuration déclarative et
 * vérifiable.</p>
 *
 * <p>Règles d'honnêteté appliquées :</p>
 * <ul>
 *   <li><b>Désactivé par défaut</b> : sans clé, le fournisseur reste
 *       « non configuré » et répond 503 {@code STT_NOT_CONFIGURED} — jamais une
 *       transcription inventée.</li>
 *   <li><b>La clé n'est jamais exposée</b> : {@link #toString()} et
 *       {@link #reason()} ne contiennent que {@link #redactedApiKey()}.</li>
 *   <li><b>Échec bruyant</b> : activer la STT sans URL ou sans clé, ou
 *       fournir une URL non http(s), fait échouer le démarrage.</li>
 * </ul>
 *
 * <p>Enregistrement : {@link AiConfiguration}.</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.speech")
public class SpeechToTextProperties {

    /** URL http(s) valide, ou chaîne vide (= « non configuré »). */
    private static final String HTTP_URL_OR_BLANK = "^$|^https?://\\S+$";

    /** Marqueur affiché à la place de la clé : ne révèle ni la valeur ni sa longueur. */
    public static final String REDACTED = "***redacted***";

    /** Modèle Whisper par défaut. */
    public static final String DEFAULT_MODEL = "whisper-1";

    /** Timeout par défaut : une transcription de 25 Mo peut légitimement dépasser 30 s. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /** Bascule générale : autoriser l'appel à un fournisseur STT réel. */
    private boolean enabled = false;

    /** URL de base compatible OpenAI (ex : https://api.openai.com/v1). */
    @Pattern(regexp = HTTP_URL_OR_BLANK,
            message = "app.speech.api-url doit être une URL http(s) valide (ex : https://api.openai.com/v1) ou vide")
    private String apiUrl = "";

    /** Clé d'API. Jamais journalisée, jamais exposée, jamais incluse dans un message d'erreur. */
    private String apiKey = "";

    /** Modèle de transcription (ex : whisper-1). */
    @NotBlank(message = "app.speech.model ne peut pas être vide (ex : whisper-1)")
    private String model = DEFAULT_MODEL;

    /** Timeout d'une transcription. Un nombre seul est interprété en secondes. */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout = DEFAULT_TIMEOUT;

    /**
     * Taille maximale d'un envoi audio, en octets (defaut 25 MiB).
     *
     * <p>Plafonne <b>dure</b> : un upload non borne est un DoS. La valeur est
     * exposee publiquement car elle doit etre annoncee au client en cas de
     * refus (413) — elle ne contient aucun secret.
     */
    @Min(1)
    @Max(104857600)   // 100 MiB : au-dela, ce n'est plus de l'audio
    private long maxFileBytes = 25L * 1024 * 1024;   // 25 MiB

    /**
     * Accès explicite à la clé. Réservé à l'adaptateur HTTP qui doit envoyer
     * l'en-tête d'authentification : ni un log, ni une réponse d'API, ni un
     * diagnostic ne doivent passer par ici.
     */
    @JsonIgnore
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Indique si la dictée vocale est réellement utilisable : bascule activée,
     * endpoint nommé, clé présente et timeout exploitable.
     */
    public boolean isConfigured() {
        return enabled
                && OllamaProperties.isNotBlank(apiUrl)
                && OllamaProperties.isHttpUrl(apiUrl)
                && OllamaProperties.isNotBlank(apiKey)
                && OllamaProperties.isNotBlank(model)
                && isPositiveTimeout();
    }

    /**
     * Explication précise de l'absence de configuration, ou chaîne vide si la
     * dictée vocale est utilisable. La valeur de la clé n'y figure jamais.
     */
    public String reason() {
        if (!enabled) {
            return "Dictée vocale (STT) désactivée : app.speech.enabled=false. "
                    + "Aucun fournisseur de transcription n'est appelé.";
        }
        if (!OllamaProperties.isNotBlank(apiUrl)) {
            return "Dictée vocale (STT) activée mais app.speech.api-url est vide : "
                    + "l'endpoint du fournisseur de transcription est inconnu.";
        }
        if (!OllamaProperties.isHttpUrl(apiUrl)) {
            return "Dictée vocale (STT) activée mais app.speech.api-url n'est pas une URL http(s) valide.";
        }
        if (!OllamaProperties.isNotBlank(apiKey)) {
            return "Dictée vocale (STT) activée mais app.speech.api-key est absente : "
                    + "aucune authentification possible. Renseignez APP_SPEECH_API_KEY.";
        }
        if (!OllamaProperties.isNotBlank(model)) {
            return "Dictée vocale (STT) activée mais app.speech.model est vide : aucun modèle à demander.";
        }
        if (!isPositiveTimeout()) {
            return "Dictée vocale (STT) activée mais app.speech.timeout doit être strictement positif.";
        }
        return "";
    }

    /**
     * Clé masquée pour affichage : {@link #REDACTED} si une clé existe,
     * {@code null} sinon. La longueur réelle n'est jamais déduite.
     */
    public String redactedApiKey() {
        return OllamaProperties.isNotBlank(apiKey) ? REDACTED : null;
    }

    @AssertTrue(message = "app.speech.enabled=true exige app.speech.api-url ET app.speech.api-key non vides")
    public boolean isProviderCompleteWhenEnabled() {
        return !enabled || (OllamaProperties.isNotBlank(apiUrl) && OllamaProperties.isNotBlank(apiKey));
    }

    @AssertTrue(message = "app.speech.timeout doit être strictement positif (ex : 60s)")
    public boolean isTimeoutStrictlyPositive() {
        return isPositiveTimeout();
    }

    private boolean isPositiveTimeout() {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    /**
     * Représentation sûre : la clé y est remplacée par {@link #REDACTED}.
     */
    @Override
    public String toString() {
        return "SpeechToTextProperties{enabled=" + enabled
                + ", apiUrl='" + apiUrl + "'"
                + ", apiKey=" + redactedApiKey()
                + ", model='" + model + "'"
                + ", timeout=" + timeout + '}';
    }
}
