package com.discipolat.common.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que les clés annoncées pour {@code application.yml} sont réellement
 * liées par Spring Boot, et qu'une valeur fausse fait échouer le démarrage.
 *
 * <p>Utilise le {@link Binder} de Spring Boot — le même chemin que
 * {@code @ConfigurationProperties} au boot — sans démarrer de contexte ni
 * toucher au réseau. Le {@code ValidationBindHandler} est recreé à chaque
 * liaison car il accumule les violations d'un appel à l'autre.</p>
 */
class AiConfigurationPropertiesBindingTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    private static BindHandler validation() {
        return new ValidationBindHandler(new SpringValidatorAdapter(validator));
    }

    private static <T> T bind(Map<String, String> values, String prefix, Class<T> type) {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(new LinkedHashMap<>(values));
        return new Binder(java.util.List.of(source)).bindOrCreate(prefix, Bindable.of(type), validation());
    }

    @Test
    void ollamaStaysDisabledWhenNoKeyIsPresentInTheYaml() {
        OllamaProperties properties = bind(Map.of(), "app.ollama", OllamaProperties.class);

        assertFalse(properties.isEnabled());
        assertFalse(properties.isConfigured(), "sans app.ollama.* dans le yml, l'IA doit être absente");
        assertEquals("http://localhost:11434", properties.getUrl());
        assertEquals("llama3", properties.getModel());
        assertEquals(Duration.ofSeconds(30), properties.getTimeout());
    }

    @Test
    void ollamaYamlKeysBindExactlyAsDocumented() {
        Map<String, String> yaml = new LinkedHashMap<>();
        yaml.put("app.ollama.enabled", "true");
        yaml.put("app.ollama.url", "http://ollama.internal:11434");
        yaml.put("app.ollama.model", "qwen2.5");
        yaml.put("app.ollama.timeout", "45");

        OllamaProperties properties = bind(yaml, "app.ollama", OllamaProperties.class);

        assertTrue(properties.isConfigured());
        assertEquals("qwen2.5", properties.getModel());
        assertEquals(Duration.ofSeconds(45), properties.getTimeout(),
                "un timeout en nombre seul doit être lu en secondes");
    }

    @Test
    void ollamaTimeoutAcceptsIso8601Too() {
        OllamaProperties properties = bind(
                Map.of("app.ollama.timeout", "90s"), "app.ollama", OllamaProperties.class);

        assertEquals(Duration.ofSeconds(90), properties.getTimeout());
    }

    @Test
    void nonHttpOllamaUrlFailsTheBoot() {
        assertBindFails("app.ollama.url",
                Map.of("app.ollama.enabled", "true", "app.ollama.url", "ollama:11434"),
                "app.ollama", OllamaProperties.class);
    }

    @Test
    void nonPositiveOllamaTimeoutFailsTheBoot() {
        assertBindFails("app.ollama.timeout",
                Map.of("app.ollama.enabled", "true", "app.ollama.timeout", "0s"),
                "app.ollama", OllamaProperties.class);
    }

    @Test
    void speechYamlKeysBindExactlyAsDocumented() {
        Map<String, String> yaml = new LinkedHashMap<>();
        yaml.put("app.speech.enabled", "true");
        yaml.put("app.speech.api-url", "https://api.openai.com/v1");
        yaml.put("app.speech.api-key", "sk-live-never-logged");
        yaml.put("app.speech.model", "whisper-1");
        yaml.put("app.speech.timeout", "30s");

        SpeechToTextProperties properties = bind(yaml, "app.speech", SpeechToTextProperties.class);

        assertTrue(properties.isConfigured());
        assertEquals("https://api.openai.com/v1", properties.getApiUrl());
        assertFalse(properties.toString().contains("sk-live-never-logged"),
                "la clé ne doit pas sortir du toString : " + properties);
    }

    @Test
    void speechIsAbsentWhenTheYamlDeclaresNoKey() {
        SpeechToTextProperties properties = bind(Map.of(), "app.speech", SpeechToTextProperties.class);

        assertFalse(properties.isEnabled());
        assertFalse(properties.isConfigured());
    }

    @Test
    void blankSpeechModelFailsTheBoot() {
        assertBindFails("app.speech.model",
                Map.of("app.speech.enabled", "true",
                        "app.speech.api-url", "https://api.openai.com/v1",
                        "app.speech.api-key", "sk-live-never-logged",
                        "app.speech.model", ""),
                "app.speech", SpeechToTextProperties.class);
    }

    @Test
    void enablingSpeechWithoutKeyFailsTheBoot() {
        assertBindFails("app.speech.api-key",
                Map.of("app.speech.enabled", "true",
                        "app.speech.api-url", "https://api.openai.com/v1"),
                "app.speech", SpeechToTextProperties.class);
    }

    /**
     * Spring Boot enveloppe les violations de validation de liaison dans une
     * {@code BindException} : on vérifie le message, qui doit nommer la clé
     * fautive pour que l'erreur de déploiement soit actionnable.
     */
    private static void assertBindFails(String expectedKey, Map<String, String> values, String prefix, Class<?> type) {
        BindException thrown = assertThrows(BindException.class, () -> bind(values, prefix, type));
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        assertTrue(messages.toString().contains(expectedKey),
                "le message doit nommer " + expectedKey + ", obtenu : " + messages);
    }
}
