package com.discipolat.modules.notifications.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P0 — Contenu d'une notification push destinée aux appareils mobiles.
 *
 * <p>Le contrat est volontairement minimal : un titre, un corps et un dictionnaire
 * de données (payload de navigation). Aucun secret, aucune référence à un
 * fournisseur : c'est {@link PushGateway} qui décide du transport.</p>
 *
 * <p>Firebase impose des valeurs {@code String} non nulles : le constructeur
 * normalise donc les valeurs nulles plutôt que de laisser remonter une
 * {@code NullPointerException} au moment de l'envoi.</p>
 *
 * @param title titre affiché sur l'appareil (peut être vide)
 * @param body  corps du message (peut être vide)
 * @param data  données de navigation transmises à l'application
 */
public record PushMessage(String title, String body, Map<String, String> data) {

    public PushMessage {
        data = sanitize(data);
    }

    public PushMessage(String title, String body) {
        this(title, body, Map.of());
    }

    /** {@code true} si le message ne porte ni titre ni corps : envoi sans contenu visible. */
    public boolean isEmpty() {
        return isBlank(title) && isBlank(body);
    }

    private static Map<String, String> sanitize(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        data.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                out.put(key, value);
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
