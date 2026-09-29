package com.discipolat.common.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrat de {@link SpeechToTextProperties} — M5 (config STT/Whisper absente de
 * {@code application.yml}).
 *
 * <p>Tests purement unitaires : aucun contexte Spring, aucun appel réseau.</p>
 */
class SpeechToTextPropertiesTest {

    private static final String SECRET = "sk-stt-super-secret-value";

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

    private static SpeechToTextProperties fullyConfigured() {
        SpeechToTextProperties properties = new SpeechToTextProperties();
        properties.setEnabled(true);
        properties.setApiUrl("https://api.openai.com/v1");
        properties.setApiKey(SECRET);
        properties.setModel("whisper-1");
        properties.setTimeout(Duration.ofSeconds(60));
        return properties;
    }

    private static SpeechToTextProperties tap(SpeechToTextProperties properties, Consumer<SpeechToTextProperties> mutator) {
        mutator.accept(properties);
        return properties;
    }

    @Test
    void notConfiguredByDefault() {
        SpeechToTextProperties properties = new SpeechToTextProperties();

        assertFalse(properties.isEnabled(), "la dictée vocale doit être désactivée par défaut");
        assertFalse(properties.isConfigured(), "sans clé, aucun fournisseur STT ne doit se croire actif");
        assertTrue(properties.reason().contains("app.speech.enabled"), properties.reason());
    }

    @Test
    void defaultsMatchTheExistingWhisperProvider() {
        SpeechToTextProperties properties = new SpeechToTextProperties();

        assertEquals("whisper-1", properties.getModel());
        assertEquals("", properties.getApiUrl());
        assertEquals("", properties.getApiKey());
        assertEquals(Duration.ofSeconds(60), properties.getTimeout());
    }

    @Test
    void isConfiguredOnlyWhenEndpointAndKeyAreBothPresent() {
        assertTrue(fullyConfigured().isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setApiKey("  ")).isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setApiUrl("")).isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setApiUrl("api.openai.com")).isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setModel("")).isConfigured());
        assertFalse(tap(fullyConfigured(), p -> p.setEnabled(false)).isConfigured());
    }

    @Test
    void apiKeyIsRedactedInToString() {
        String rendered = fullyConfigured().toString();

        assertFalse(rendered.contains(SECRET), "la clé ne doit jamais être journalisée : " + rendered);
        assertTrue(rendered.contains(SpeechToTextProperties.REDACTED), rendered);
    }

    @Test
    void redactedAccessorNeverRevealsTheKeyNorItsLength() {
        assertEquals(SpeechToTextProperties.REDACTED, fullyConfigured().redactedApiKey());
        assertEquals(SpeechToTextProperties.REDACTED, fullyConfigured().redactedApiKey(),
                "la longueur réelle de la clé ne doit pas être déductible");
        assertNull(new SpeechToTextProperties().redactedApiKey());
    }

    @Test
    void reasonNeverExposesTheApiKey() {
        SpeechToTextProperties properties = new SpeechToTextProperties();
        properties.setEnabled(true);
        properties.setApiUrl("https://api.openai.com/v1");
        properties.setApiKey(SECRET);
        properties.setModel("");

        String reason = properties.reason();

        assertFalse(reason.contains(SECRET), "le motif ne doit pas divulguer la clé : " + reason);
        assertFalse(reason.toLowerCase(Locale.ROOT).contains("super-secret"), reason);
        assertTrue(reason.contains("app.speech.model"), reason);
        assertFalse(fullyConfigured().reason().contains(SECRET));
    }

    @Test
    void reasonIsExplicitAndNonEmptyInEveryMisconfiguredCase() {
        record Case(String label, Consumer<SpeechToTextProperties> mutator, String expectedKey) {
        }

        List<Case> cases = List.of(
                new Case("désactivé", p -> p.setEnabled(false), "app.speech.enabled"),
                new Case("url vide", p -> p.setApiUrl(""), "app.speech.api-url"),
                new Case("url non http", p -> p.setApiUrl("api.openai.com"), "app.speech.api-url"),
                new Case("clé absente", p -> p.setApiKey(""), "app.speech.api-key"),
                new Case("modèle vide", p -> p.setModel(""), "app.speech.model"),
                new Case("timeout nul", p -> p.setTimeout(Duration.ZERO), "app.speech.timeout")
        );

        for (Case testCase : cases) {
            SpeechToTextProperties properties = tap(fullyConfigured(), testCase.mutator());
            String reason = properties.reason();

            assertFalse(properties.isConfigured(), testCase.label() + " : ne doit pas être configuré");
            assertFalse(reason.isBlank(), testCase.label() + " : motif vide");
            assertTrue(reason.contains(testCase.expectedKey()),
                    testCase.label() + " : le motif doit nommer " + testCase.expectedKey() + ", obtenu : " + reason);
        }

        assertEquals("", fullyConfigured().reason(), "une configuration STT valide n'a rien à signaler");
    }

    @Test
    void validationRejectsBlankModelWhenEnabled() {
        SpeechToTextProperties properties = tap(fullyConfigured(), p -> p.setModel("   "));

        Set<String> messages = validator.validate(properties).stream()
                .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                .collect(Collectors.toSet());

        assertTrue(messages.stream().anyMatch(m -> m.contains("model")),
                "un modèle vide doit faire échouer la validation : " + messages);
        assertTrue(messages.stream().anyMatch(m -> m.contains("app.speech.model")), messages.toString());
    }

    @Test
    void validationRejectsNonHttpEndpoint() {
        SpeechToTextProperties properties = tap(fullyConfigured(), p -> p.setApiUrl("api.openai.com/v1"));

        assertTrue(validator.validate(properties).stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("apiUrl")),
                "une URL non http(s) doit être refusée, pas corrigée en silence");
    }

    @Test
    void validationRejectsEnabledWithoutKey() {
        SpeechToTextProperties properties = new SpeechToTextProperties();
        properties.setEnabled(true);
        properties.setApiUrl("https://api.openai.com/v1");

        Set<String> messages = validator.validate(properties).stream()
                .map(v -> v.getMessage())
                .collect(Collectors.toSet());

        assertTrue(messages.stream().anyMatch(m -> m.contains("app.speech.api-key")),
                "activer la STT sans clé doit être bruyant : " + messages);
    }

    @Test
    void validationRejectsNonPositiveTimeout() {
        SpeechToTextProperties properties = tap(fullyConfigured(), p -> p.setTimeout(Duration.ofSeconds(-1)));

        assertTrue(validator.validate(properties).stream()
                        .anyMatch(v -> v.getMessage().contains("app.speech.timeout")),
                "un timeout négatif doit être refusé");
    }

    @Test
    void validConfigurationPassesValidation() {
        assertTrue(validator.validate(fullyConfigured()).isEmpty(),
                "une configuration STT valide ne doit produire aucune violation");
    }

    @Test
    void apiKeyIsReadableOnlyThroughAnExplicitNonSerializableAccessor() throws Exception {
        assertEquals(SECRET, fullyConfigured().getApiKey(),
                "l'adaptateur HTTP doit pouvoir lire la clé via l'accesseur explicite");

        Method accessor = SpeechToTextProperties.class.getDeclaredMethod("getApiKey");
        assertNotNull(accessor.getAnnotation(JsonIgnore.class),
                "l'accesseur doit porter @JsonIgnore : aucun endpoint de diagnostic ne doit sérialiser la clé");
    }
}
