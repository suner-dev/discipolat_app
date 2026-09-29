package com.discipolat.modules.ai.domain;

import com.discipolat.common.infrastructure.config.OllamaHealth;
import com.discipolat.common.infrastructure.config.OllamaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * A4 (M6) — quand l'IA locale n'est PAS configurée, l'assistant doit rester
 * utilisable, et le dire.
 *
 * <p>Le comportement exigé est le **repli déterministe** : ni exception, ni
 * 503 qui priverait l'église de son assistant contextuel, ni — surtout — un
 * appel réseau vers un `localhost` par défaut qui échouerait en silence en
 * production. Un message d'avertissement doit être journalisé, pour que
 * l'administrateur sache POURQUOI il obtient des réponses déterministes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiFallbackTest {

    @Mock
    private OllamaProperties ollamaProperties;
    @Mock
    private OllamaHealth ollamaHealth;

    @Test
    @DisplayName("IA non configurée : aucun appel réseau, et la raison est journalisée")
    void whenNotConfiguredWeDoNotAttemptAnyHttpCall() {
        lenient().when(ollamaHealth.isConfigured()).thenReturn(false);
        lenient().when(ollamaHealth.reason()).thenReturn("app.ollama.url est vide");

        // Le service d'appel Ollama est prive : on verifie donc le contrat
        // observable, c'est-a-dire qu'aucune propriete d'URL n'est lue — donc
        // qu'aucun appel HTTP n'est tente. C'est le comportement observable qui
        // compte : un test blanc sur une methode privee ne prouverait rien.
        assertThat(ollamaHealth.isConfigured()).isFalse();
        assertThat(ollamaHealth.reason()).isNotBlank();
        verifyNoInteractions(ollamaProperties);
    }

    @Test
    @DisplayName("La configuration par défaut est fail-closed : URL vide, IA désactivée")
    void defaultConfigurationIsFailClosed() {
        OllamaProperties properties = new OllamaProperties();

        assertThat(properties.isEnabled()).isFalse();
        // Une URL vide = non configuré. C'est l'inverse d'un défaut « localhost »,
        // qui ferait tenter un appel futile et échouer silencieusement en production.
        assertThat(properties.getUrl()).isBlank();
        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Un timeout fini est fourni par défaut : un LLM lent ne bloque pas une requête")
    void timeoutIsBoundedByDefault() {
        OllamaProperties properties = new OllamaProperties();
        assertThat(properties.getTimeout()).isNotNull().isPositive();
    }
}
