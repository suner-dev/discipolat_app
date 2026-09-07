package com.discipolat.modules.ussd.domain;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration du gateway USSD Africa's Talking.
 *
 * <p>Africa's Talking expose une API USSD qui permet aux utilisateurs sans smartphone
 * d'accéder aux services Discipolat via des codes USSD (ex: *384*999#).</p>
 *
 * <p>Flux USSD :
 * <ol>
 *   <li>L'utilisateur compose le code USSD sur son téléphone</li>
 *   <li>Africa's Talking notifie notre callback POST /api/v1/ussd/callback</li>
 *   <li>Le service affiche un menu, traite la navigation, exécute l'action</li>
 *   <li>La réponse texte est renvoyée à Africa's Talking qui l'affiche sur le feature phone</li>
 * </ol>
 *
 * <p>Documentation : https://developers.africastalking.com/docs/ussd/overview</p>
 */
@Component
@Getter
public class UssdProperties {

    /** Bascule générale : activer le gateway USSD Africa's Talking. */
    @Value("${app.ussd.enabled:false}")
    private boolean enabled;

    /** Clé API Africa's Talking (API Key). */
    @Value("${app.ussd.api-key:}")
    private String apiKey;

    /** Nom d'utilisateur Africa's Talking (username). */
    @Value("${app.ussd.username:}")
    private String username;

    /** Code du service USSD (fourni par Africa's Talking après enregistrement). */
    @Value("${app.ussd.service-code:}")
    private String serviceCode;

    /** URL de base de l'API Africa's Talking. */
    @Value("${app.ussd.base-url:https://api.africastalking.com}")
    private String baseUrl;

    /** Secret pour valider les callbacks entrants (sécurité). */
    @Value("${app.ussd.webhook-secret:}")
    private String webhookSecret;

    /** URL publique du callback USSD (exposée à Africa's Talking). */
    @Value("${app.ussd.callback-url:}")
    private String callbackUrl;

    /**
     * Indique si le gateway USSD est réellement configuré (clé + username + service code).
     */
    public boolean isConfigured() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && username != null && !username.isBlank()
                && serviceCode != null && !serviceCode.isBlank();
    }
}
