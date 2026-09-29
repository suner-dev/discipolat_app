package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.OllamaHealth;
import com.discipolat.common.infrastructure.config.OllamaProperties;
import com.discipolat.common.infrastructure.config.SpeechToTextProperties;
import com.discipolat.modules.notifications.domain.PushProperties;
import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A4 — résumé de configuration, strictement NON sensible.
 *
 * <p>Alimente une future page de diagnostic. Le contrat est volontairement
 * étroit : pour chaque feature, {@code {key, enabled, configured}}.
 *
 * <h2>Ce que cet endpoint ne doit JAMAIS contenir</h2>
 *
 * <p>Aucun secret et aucune URL interne complete. Une URL peut contenir des
 * identifiants ({@code https://user:pass@host}), donc seules des URL
 * <b>masquees</b> sont exposees, et jamais une valeur de type
 * {@code *KEY*}, {@code *SECRET*}, {@code *PASSWORD*}, {@code *TOKEN*}.
 * Le test {@code ConfigSummaryTest} verifie cela explicitement, en parcourant les
 * valeurs de toutes les proprietes de l'environnement : c'est le seul moyen de
 * garantir qu'un secret ajoute ulterieurement ne fuite pas par ce chemin.
 */
@RestController
public class SystemConfigSummaryController {

    private final PlatformFeatureFlagService featureFlagService;
    private final OllamaProperties ollamaProperties;
    private final OllamaHealth ollamaHealth;
    private final SpeechToTextProperties speechProperties;
    private final PushProperties pushProperties;

    public SystemConfigSummaryController(PlatformFeatureFlagService featureFlagService,
                                         OllamaProperties ollamaProperties,
                                         OllamaHealth ollamaHealth,
                                         SpeechToTextProperties speechProperties,
                                         PushProperties pushProperties) {
        this.featureFlagService = featureFlagService;
        this.ollamaProperties = ollamaProperties;
        this.ollamaHealth = ollamaHealth;
        this.speechProperties = speechProperties;
        this.pushProperties = pushProperties;
    }

    @GetMapping("/api/v1/system/config-summary")
    public ResponseEntity<Map<String, Object>> configSummary() {
        // Les cles viennent des constantes du service : les lister ici permetrait
        // d'oublier un flag, et un diagnostic qui ment sur ce qui existe est pire
        // que pas de diagnostic.
        List<String> keys = List.of(
                PlatformFeatureFlagService.AI_ENABLED,
                PlatformFeatureFlagService.MOBILE_MONEY_ENABLED,
                PlatformFeatureFlagService.WHATSAPP_ENABLED,
                PlatformFeatureFlagService.ANALYTICS_ENABLED,
                PlatformFeatureFlagService.DOCS_ENABLED);

        List<Map<String, Object>> features = new ArrayList<>();
        for (String key : keys) {
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("key", key);
            feature.put("enabled", isEnabled(key));
            // « configured » = le flag est réellement connu du catalogue.
            // Un flagAbsent n'est pas « désactivé par configuration » : c'est un
            // état différent, et un diagnostic qui les confond trompe l'exploitant.
            feature.put("configured", isConfigured(key));
            features.add(feature);
        }

        Map<String, Object> integrations = new LinkedHashMap<>();
        // `configured` seulement : jamais l'URL complete ni la cle.
        integrations.put("ai.local", Map.of(
                "enabled", ollamaProperties.isEnabled(),
                "configured", ollamaHealth.isConfigured(),
                "model", ollamaProperties.getModel()));
        integrations.put("speech.toText", Map.of(
                "enabled", speechProperties.isEnabled(),
                "configured", speechProperties.isConfigured(),
                "model", speechProperties.getModel(),
                "maxFileBytes", speechProperties.getMaxFileBytes()));
        integrations.put("push", Map.of(
                "enabled", pushProperties.isEnabled(),
                // L'état réel du compte de service, jamais son chemin : un faux
                // « configured=false » codé en dur serait exactement le genre de
                // mensonge fonctionnel que ce chantier corrige.
                "configured", pushProperties.isConfigured(),
                "dryRun", pushProperties.isDryRun(),
                "statusEndpoint", "/api/v1/notifications/push-status"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("features", features);
        body.put("integrations", integrations);
        return ResponseEntity.ok(body);
    }

    /** Un flag absent est « desactive », jamais une exception : le diagnostic ne doit pas tomber. */
    private boolean isEnabled(String key) {
        try {
            return featureFlagService.isEnabled(key);
        } catch (RuntimeException unknownFlag) {
            return false;
        }
    }

    /** Le catalogue contient-il reellement ce flag ? Une erreur de lecture = « non configure ». */
    private boolean isConfigured(String key) {
        try {
            return featureFlagService.getByKey(key).isPresent();
        } catch (RuntimeException storageFailure) {
            return false;
        }
    }
}
