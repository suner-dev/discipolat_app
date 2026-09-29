package com.discipolat.modules.notifications.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P0 — Sélection de la passerelle push.
 *
 * <p>Une seule règle, volontairement explicite et testable :</p>
 * <pre>
 *   app.push.enabled = false            → NoOpPushGateway        (défaut)
 *   app.push.enabled = true, non config. → NoOpPushGateway        (raison journalisée)
 *   app.push.enabled = true, configuré  → FirebaseAdminPushGateway
 * </pre>
 *
 * <p>Aucun autre cas : il n'existe pas de « push activé mais silencieusement
 * perdu ». Si le compte de service manque, l'application le dit au démarrage
 * et l'endpoint {@code /notifications/push-status} l'expose.</p>
 *
 * <p>La règle est dans {@link #select(PushProperties)} (méthode statique) afin
 * d'être vérifiable sans démarrer un contexte Spring.</p>
 */
@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PushGatewayConfiguration.class);

    @Bean
    public PushGateway pushGateway(PushProperties properties) {
        PushGateway gateway = select(properties);
        log.info("[Push] Passelle sélectionnée : {} — {}", gateway.getClass().getSimpleName(), properties.reason());
        return gateway;
    }

    /**
     * Règle de sélection —cf. la JavaDoc de la classe.
     *
     * @param properties configuration {@code app.push.*}
     * @return la passerelle correspondant à l'état réel de la configuration
     */
    public static PushGateway select(PushProperties properties) {
        if (!properties.isEnabled()) {
            return new NoOpPushGateway(properties.reason());
        }
        if (!properties.isConfigured()) {
            return new NoOpPushGateway(properties.reason());
        }
        return new FirebaseAdminPushGateway(properties);
    }
}
