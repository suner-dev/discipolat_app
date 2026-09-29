package com.discipolat.common.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrat du garde-fou IA : {@link OllamaHealth}.
 *
 * <p>Tests purement unitaires : aucun contexte Spring, aucun appel réseau. Le
 * composant ne possède d'ailleurs aucun client HTTP — ce qui est vérifié
 * explicitement par {@link #declaresNoHttpClient()}.</p>
 */
class OllamaHealthTest {

    private static OllamaHealth healthWith(Consumer<OllamaProperties> mutator) {
        OllamaProperties properties = new OllamaProperties();
        mutator.accept(properties);
        return new OllamaHealth(properties);
    }

    private static OllamaHealth configuredHealth() {
        return healthWith(p -> {
            p.setEnabled(true);
            p.setUrl("http://ollama.internal:11434");
            p.setModel("llama3");
            p.setTimeout(Duration.ofSeconds(30));
        });
    }

    @Test
    void disabledByDefaultIsNotConfiguredAndSaysWhy() {
        OllamaHealth health = new OllamaHealth(new OllamaProperties());

        assertFalse(health.isConfigured());
        assertTrue(health.reason().contains("app.ollama.enabled"),
                "le motif doit nommer la clé responsable : " + health.reason());
    }

    @Test
    void reasonIsExplicitAndNonEmptyInEveryMisconfiguredCase() {
        record Case(String label, Consumer<OllamaProperties> mutator, String expectedKey) {
        }

        List<Case> cases = List.of(
                new Case("désactivé", p -> p.setEnabled(false), "app.ollama.enabled"),
                new Case("url vide", p -> {
                    p.setEnabled(true);
                    p.setUrl("");
                }, "app.ollama.url"),
                new Case("url blanche", p -> {
                    p.setEnabled(true);
                    p.setUrl("   ");
                }, "app.ollama.url"),
                new Case("url non http", p -> {
                    p.setEnabled(true);
                    p.setUrl("ollama:11434");
                }, "app.ollama.url"),
                new Case("modèle vide", p -> {
                    p.setEnabled(true);
                    // URL explicitement fournie : sinon c'est elle qui est
                    // fautive, et c'est elle que le motif doit nommer. Un motif
                    // qui n'annoncerait qu'un défaut parmi plusieurs laisse
                    // l'administrateur deviner le reste.
                    p.setUrl("http://ollama.exemple:11434");
                    p.setModel("");
                }, "app.ollama.model"),
                new Case("timeout nul", p -> {
                    p.setEnabled(true);
                    p.setUrl("http://ollama.exemple:11434");
                    p.setTimeout(Duration.ZERO);
                }, "app.ollama.timeout"),
                new Case("timeout négatif", p -> {
                    p.setEnabled(true);
                    p.setUrl("http://ollama.exemple:11434");
                    p.setTimeout(Duration.ofSeconds(-5));
                }, "app.ollama.timeout"),
                new Case("timeout absent", p -> {
                    p.setEnabled(true);
                    p.setUrl("http://ollama.exemple:11434");
                    p.setTimeout(null);
                }, "app.ollama.timeout")
        );

        // Le defaut etant desormais une URL VIDE, le premier motif encountered est
        // celui de l'URL : c'est correct, et c'est meme l'information la plus utile.
        for (Case testCase : cases) {
            OllamaHealth health = healthWith(testCase.mutator());
            String reason = health.reason();

            assertFalse(health.isConfigured(), testCase.label() + " : ne doit pas être configuré");
            assertNotNull(reason, testCase.label() + " : le motif ne doit pas être nul");
            assertFalse(reason.isBlank(), testCase.label() + " : le motif ne doit pas être vide");
            assertTrue(reason.contains(testCase.expectedKey()),
                    testCase.label() + " : le motif doit nommer " + testCase.expectedKey() + ", obtenu : " + reason);
        }
    }

    @Test
    void configuredHealthHasNoReasonToReport() {
        OllamaHealth health = configuredHealth();

        assertTrue(health.isConfigured());
        assertEquals("", health.reason(), "une IA correctement configurée n'a rien à signaler");
    }

    @Test
    void requireConfiguredIsSilentWhenTheAiIsUsable() {
        assertDoesNotThrow(configuredHealth()::requireConfigured);
    }

    @Test
    void requireConfiguredFailsWith503NotConfiguredInsteadOfAConnectionError() {
        OllamaHealth health = healthWith(p -> p.setEnabled(false));

        AiNotConfiguredException thrown = assertThrows(AiNotConfiguredException.class, health::requireConfigured);

        ProblemDetail problem = thrown.toProblemDetail();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problem.getStatus());
        assertEquals(AiNotConfiguredException.CODE, problem.getTitle());
        assertEquals("AI_NOT_CONFIGURED", AiNotConfiguredException.CODE);
        assertTrue(problem.getDetail().contains("app.ollama.enabled"),
                "le client doit savoir quoi corriger : " + problem.getDetail());
    }

    @Test
    void statusIsPublishableAndMasksTheUrl() {
        OllamaHealth health = healthWith(p -> {
            p.setEnabled(true);
            p.setUrl("http://user:super-secret@ollama.internal:11434");
        });

        String rendered = String.valueOf(health.status());

        assertEquals(true, health.status().get("configured"));
        assertFalse(rendered.contains("super-secret"), "aucun credential dans un diagnostic : " + rendered);
        assertTrue(rendered.contains("***"), "l'URL doit être masquée : " + rendered);
    }

    @Test
    void statusCarriesTheReasonWhenNotConfigured() {
        OllamaHealth health = healthWith(p -> p.setEnabled(false));

        assertEquals(false, health.status().get("configured"));
        assertTrue(String.valueOf(health.status().get("reason")).contains("app.ollama.enabled"));
    }

    @Test
    void statusNeverLeaksCredentials() {
        OllamaHealth health = configuredHealth();

        String rendered = String.valueOf(health.status()).toLowerCase(Locale.ROOT);

        for (String forbidden : Set.of("apikey", "secret", "token", "bearer", "password")) {
            assertFalse(rendered.contains(forbidden), "statut IA : « " + forbidden + " » interdit");
        }
    }

    /**
     * Le garde-fou doit rester une vérification de configuration pure : si un
     * client HTTP réapparaissait dans ce composant, un serveur IA lent
     * pourrait bloquer le démarrage.
     */
    @Test
    void declaresNoHttpClient() {
        for (Field field : OllamaHealth.class.getDeclaredFields()) {
            String type = field.getType().getName().toLowerCase(Locale.ROOT);
            assertFalse(type.contains("resttemplate"), "champ HTTP interdit : " + field);
            assertFalse(type.contains("restclient"), "champ HTTP interdit : " + field);
            assertFalse(type.contains("webclient"), "champ HTTP interdit : " + field);
            assertFalse(type.contains("httpclient"), "champ HTTP interdit : " + field);
        }
    }
}
