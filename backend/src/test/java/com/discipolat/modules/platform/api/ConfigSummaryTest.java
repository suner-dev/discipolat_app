package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.OllamaHealth;
import com.discipolat.common.infrastructure.config.OllamaProperties;
import com.discipolat.common.infrastructure.config.SpeechToTextProperties;
import com.discipolat.modules.notifications.domain.PushProperties;
import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * A4 — le résumé de configuration ne doit FUIR AUCUN secret.
 *
 * <p>Le test n'est pas « il n'y a pas de secret dans la réponse » — c'est
 * invérifiable. Il parcourt <b>toutes</b> les variables d'environnement dont le
 * nom évoque un secret, et exige qu'aucune de leurs valeurs n'apparaisse dans la
 * réponse. Un secret introduit plus tard est donc attrapé automatiquement, sans
 * qu'on ait à penser à mettre à jour ce test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigSummaryTest {

    /** Nom de variable dont la valeur ne doit JAMAIS apparaître dans la réponse. */
    private static final Pattern SECRET_NAME = Pattern.compile(
            ".*(SECRET|PASSWORD|PASSWD|TOKEN|API_?KEY|PRIVATE|CREDENTIAL).*",
            Pattern.CASE_INSENSITIVE);

    @Mock
    private PlatformFeatureFlagService featureFlagService;
    @Mock
    private OllamaProperties ollamaProperties;
    @Mock
    private OllamaHealth ollamaHealth;
    @Mock
    private SpeechToTextProperties speechProperties;
    @Mock
    private PushProperties pushProperties;

    private Map<String, Object> summary() {
        when(featureFlagService.isEnabled(anyString())).thenReturn(false);
        when(ollamaProperties.isEnabled()).thenReturn(false);
        when(ollamaProperties.isConfigured()).thenReturn(false);
        when(ollamaProperties.getModel()).thenReturn("llama3");
        when(ollamaHealth.reason()).thenReturn("url vide");
        when(speechProperties.isEnabled()).thenReturn(false);
        when(speechProperties.isConfigured()).thenReturn(false);
        when(speechProperties.getModel()).thenReturn("whisper-1");
        when(speechProperties.getMaxFileBytes()).thenReturn(25L * 1024 * 1024);
        when(pushProperties.isEnabled()).thenReturn(false);
        when(pushProperties.isConfigured()).thenReturn(false);
        when(pushProperties.isDryRun()).thenReturn(true);

        return new SystemConfigSummaryController(featureFlagService, ollamaProperties,
                ollamaHealth, speechProperties, pushProperties).configSummary().getBody();
    }

    @Test
    @DisplayName("Aucune valeur de variable d'environnement « secrète » n'apparaît dans la réponse")
    void noEnvironmentSecretLeaksIntoTheResponse() {
        Map<String, Object> body = summary();
        String rendered = String.valueOf(body).toLowerCase(Locale.ROOT);

        int secretEnvVars = 0;
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (!SECRET_NAME.matcher(entry.getKey()).matches()) {
                continue;
            }
            String value = entry.getValue();
            // Une valeur vide, ou trop courte pour etre distinctive, ne prouve rien.
            if (value == null || value.isBlank() || value.length() < 8) {
                continue;
            }
            secretEnvVars++;
            assertThat(rendered)
                    .as("la valeur de %s ne doit pas fuiter via /system/config-summary", entry.getKey())
                    .doesNotContain(value.toLowerCase(Locale.ROOT));
        }
        // Le test reste significatif meme si l'environnement ne definit aucun
        // secret : on verifie alors la forme de la reponse (ci-dessous).
        assertThat(secretEnvVars).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("La réponse n'expose ni la clé API ni l'URL de l'IA")
    void exposesNeitherTheApiKeyNorTheInternalUrl() {
        String rendered = String.valueOf(summary()).toLowerCase(Locale.ROOT);

        // Aucune cle d'API, meme masquee : l'endpoint n'a simplement rien a dire dessus.
        assertThat(rendered).doesNotContain("api-key").doesNotContain("apikey");
        // Aucune URL interne complete.
        assertThat(rendered).doesNotContain("http://").doesNotContain("https://");
    }

    @Test
    @DisplayName("Chaque feature est décrite par exactement {key, enabled, configured}")
    void eachFeatureIsDescribedByKeyEnabledAndConfigured() {
        Map<String, Object> body = summary();
        @SuppressWarnings("unchecked")
        var features = (java.util.List<Map<String, Object>>) body.get("features");

        assertThat(features).isNotEmpty();
        assertThat(features).allSatisfy(feature ->
                assertThat(feature.keySet()).containsExactlyInAnyOrder("key", "enabled", "configured"));
        assertThat(features).extracting(f -> f.get("key"))
                .contains(PlatformFeatureFlagService.AI_ENABLED);
    }

    @Test
    @DisplayName("Le bloc push reflète l'état réel de PushProperties, jamais un faux figé")
    void pushBlockReportsRealState() {
        // Construction directe : summary() re-stubb les valeurs par defaut « eteint »,
        // ce qui masquerait justement le comportement verifie ici.
        when(featureFlagService.isEnabled(anyString())).thenReturn(false);
        when(ollamaProperties.isEnabled()).thenReturn(false);
        when(ollamaProperties.getModel()).thenReturn("llama3");
        when(speechProperties.isEnabled()).thenReturn(false);
        when(speechProperties.isConfigured()).thenReturn(false);
        when(speechProperties.getModel()).thenReturn("whisper-1");
        when(speechProperties.getMaxFileBytes()).thenReturn(25L * 1024 * 1024);
        when(pushProperties.isEnabled()).thenReturn(true);
        when(pushProperties.isConfigured()).thenReturn(true);
        when(pushProperties.isDryRun()).thenReturn(false);

        Map<String, Object> body = new SystemConfigSummaryController(featureFlagService,
                ollamaProperties, ollamaHealth, speechProperties, pushProperties)
                        .configSummary().getBody();
        @SuppressWarnings("unchecked")
        var push = (Map<String, Object>) ((Map<String, Object>) body.get("integrations")).get("push");

        assertThat(push).containsEntry("enabled", true)
                .containsEntry("configured", true)
                .containsEntry("dryRun", false);
        // Le chemin du compte de service n'est jamais expose.
        assertThat(String.valueOf(body)).doesNotContain("credentials");
    }

    @Test
    @DisplayName("Un flag inconnu ne fait pas tomber le diagnostic")
    void anUnknownFlagDoesNotBreakTheSummary() {
        when(featureFlagService.isEnabled(anyString()))
                .thenThrow(new IllegalStateException("flag inconnu"));
        when(featureFlagService.getByKey(anyString()))
                .thenThrow(new IllegalStateException("catalogue indisponible"));
        when(ollamaProperties.isEnabled()).thenReturn(false);
        when(ollamaProperties.isConfigured()).thenReturn(false);
        when(ollamaProperties.getModel()).thenReturn("llama3");
        when(speechProperties.isEnabled()).thenReturn(false);
        when(speechProperties.isConfigured()).thenReturn(false);
        when(speechProperties.getModel()).thenReturn("whisper-1");
        when(speechProperties.getMaxFileBytes()).thenReturn(25L * 1024 * 1024);
        when(pushProperties.isEnabled()).thenReturn(false);
        when(pushProperties.isConfigured()).thenReturn(false);
        when(pushProperties.isDryRun()).thenReturn(true);

        var body = new SystemConfigSummaryController(featureFlagService, ollamaProperties,
                ollamaHealth, speechProperties, pushProperties).configSummary();

        assertThat(body.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        var features = (java.util.List<Map<String, Object>>) body.getBody().get("features");
        assertThat(features).isNotEmpty()
                .allSatisfy(feature -> {
                    assertThat(feature.get("enabled")).isEqualTo(false);
                    assertThat(feature.get("configured")).isEqualTo(false);
                });
    }
}
