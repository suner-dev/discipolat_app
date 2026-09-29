package com.discipolat.modules.notifications.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * P0 — Passelle push active par défaut : elle n'envoie rien, et le dit.
 *
 * <p>Sélectionnée quand {@code app.push.enabled=false} (valeur par défaut).
 * Un no-op silencieux serait une nouvelle version du mensonge que ce module
 * supprime : l'utilisateur_mobile s'abonne à {@code firebase_messaging} et
 * n'est jamais notifié. Cette implémentation journalise donc un
 * <b>AVERTISSEMENT au niveau WARN, une seule fois au démarrage</b>, en
 * expliquant explicitement que les notifications push sont abandonnées.</p>
 */
public class NoOpPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushGateway.class);

    /** Garantit un unique avertissement, même si le bean est reconstruit. */
    private static final AtomicBoolean STARTUP_WARNING_EMITTED = new AtomicBoolean(false);

    private final String reason;

    public NoOpPushGateway(String reason) {
        this.reason = reason;
        if (STARTUP_WARNING_EMITTED.compareAndSet(false, true)) {
            log.warn("[Push] ENVOIS PUSH ABANDONNÉS (dropped) — app.push.enabled=false. "
                    + "Aucun push ne partira vers les mobiles malgré l'abonnement firebase_messaging "
                    + "de l'application. Cause : {}.", reason);
        }
    }

    @Override
    public PushResult send(List<String> deviceTokens, PushMessage message) {
        if (deviceTokens != null && !deviceTokens.isEmpty()) {
            log.debug("[Push:no-op] {} appareil(s) non notifié(s) — {}", deviceTokens.size(), reason);
        }
        return PushResult.notSent();
    }

    /** Explication de l'absence d'envoi, exposée par {@code /notifications/push-status}. */
    public String reason() {
        return reason;
    }
}
