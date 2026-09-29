package com.discipolat.common.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrat de {@link OllamaProperties} — M6 (Ollama codé en dur sur localhost).
 *
 * <p>Tests purement unitaires : aucun contexte Spring, aucun appel réseau.</p>
 */
class OllamaPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        // Interpolateur sans EL : hibernate-validator ne déclare expresslY qu'en
        // scope « provided », l'implémentation EL n'est donc pas garantie sur le
        // classpath de test. Les annotations restent le sujet testé, pas l'EL.
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

    private static OllamaProperties fullyConfigured() {
        OllamaProperties properties = new OllamaProperties();
        properties.setEnabled(true);
        properties.setUrl("http://ollama.internal:11434");
        properties.setModel("llama3");
        properties.setTimeout(Duration.ofSeconds(30));
        return properties;
    }

    @Test
    void ollamaIsDisabledByDefault() {
        OllamaProperties properties = new OllamaProperties();

        assertFalse(properties.isEnabled(), "l'IA locale doit être désactivée par défaut");
        assertFalse(properties.isConfigured(), "une IA désactivée ne peut pas être « configurée »");
    }

    @Test
    void defaultsAreUsableButInactive() {
        OllamaProperties properties = new OllamaProperties();

        // M6 : le defaut est VIDE, pas « localhost ». Un defaut localhost en
        // production fait tenter un appel futile qui echoue en silence.
        assertEquals("", properties.getUrl());
        assertEquals("llama3", properties.getModel());
        assertEquals(Duration.ofSeconds(30), properties.getTimeout());
        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredIsFalseWhenDisabledEvenWithAValidUrl() {
        OllamaProperties properties = fullyConfigured();
        properties.setEnabled(false);

        assertFalse(properties.isConfigured(),
                "une URL valide ne vaut pas activation : c'est exactement le mensonge M6");
    }

    @Test
    void isConfiguredIsFalseWhenUrlIsBlank() {
        OllamaProperties properties = fullyConfigured();
        properties.setUrl("   ");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredIsFalseWhenUrlIsNotHttp() {
        OllamaProperties properties = fullyConfigured();
        properties.setUrl("localhost:11434");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredIsFalseWhenModelIsBlank() {
        OllamaProperties properties = fullyConfigured();
        properties.setModel("  ");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredIsFalseWhenTimeoutIsNotPositive() {
        assertFalse(tap(fullyConfigured(), p -> p.setTimeout(Duration.ZERO)).isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setTimeout(Duration.ofSeconds(-1))).isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setTimeout(null)).isConfigured());
    }

    @Test
    void isConfiguredIsTrueOnlyWhenEverythingIsProvided() {
        assertTrue(fullyConfigured().isConfigured());
    }

    @Test
    void nonHttpUrlIsRejectedByValidation() {
        OllamaProperties properties = fullyConfigured();
        properties.setUrl("ollama:11434");

        assertTrue(validator.validate(properties).stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("url")),
                "une URL non http(s) doit faire échouer la validation, pas être silencieusement corrigée");
    }

    @Test
    void blankModelIsRejectedByValidationEvenWhenEnabled() {
        OllamaProperties properties = fullyConfigured();
        properties.setModel(" ");

        assertTrue(validator.validate(properties).stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("model")),
                "un modèle vide doit faire échouer la validation");
    }

    @Test
    void enabledWithoutUrlIsRejectedByValidation() {
        OllamaProperties properties = new OllamaProperties();
        properties.setEnabled(true);
        properties.setUrl("");

        Set<String> messages = validator.validate(properties).stream()
                .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(messages.stream().anyMatch(m -> m.contains("app.ollama.url")),
                "activer Ollama sans URL doit être bruyant, attendu : " + messages);
    }

    @Test
    void nonPositiveTimeoutIsRejectedByValidation() {
        OllamaProperties properties = fullyConfigured();
        properties.setTimeout(Duration.ofSeconds(0));

        assertTrue(validator.validate(properties).stream()
                        .anyMatch(v -> v.getMessage().contains("app.ollama.timeout")),
                "un timeout nul doit être refusé");
    }

    @Test
    void validConfigurationPassesValidation() {
        assertTrue(validator.validate(fullyConfigured()).isEmpty(),
                "une configuration valide ne doit produire aucune violation");
    }

    @Test
    void toStringNeverExposesAnyCredential() {
        OllamaProperties properties = fullyConfigured();
        properties.setUrl("http://user:super-secret@ollama.internal:11434");

        String rendered = properties.toString();
        String lower = rendered.toLowerCase(Locale.ROOT);

        assertFalse(rendered.contains("super-secret"), "aucun mot de passe ne doit apparaître : " + rendered);
        assertTrue(rendered.contains("***"), "l'URL doit être masquée : " + rendered);
        for (String forbidden : Set.of("apikey", "api-key", "api_key", "secret", "token", "bearer", "password")) {
            assertFalse(lower.contains(forbidden),
                    "Ollama n'exige aucun credential, « " + forbidden + " » ne doit pas exister : " + rendered);
        }
    }

    @Test
    void maskedUrlKeepsPlainLocalDevelopmentUrlReadable() {
        // Une URL de developpement explicite reste lisible : le masquage ne sert
        // qu'a retirer des identifiants, pas a rendre l'URL illisible.
        OllamaProperties properties = new OllamaProperties();
        properties.setUrl("http://localhost:11434");

        assertEquals("http://localhost:11434", properties.maskedUrl());
    }

    @Test
    void maskedUrlIsEmptyWhenUrlIsBlank() {
        OllamaProperties properties = new OllamaProperties();
        properties.setUrl("  ");

        assertEquals("", properties.maskedUrl());
    }

    /**
     * Petit utilitaire de test : évite de multiplier les variables locales.
     */
    private static <T> T tap(T value, java.util.function.Consumer<T> mutator) {
        mutator.accept(value);
        return value;
    }}
