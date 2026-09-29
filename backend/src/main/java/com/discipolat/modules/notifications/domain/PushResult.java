package com.discipolat.modules.notifications.domain;

import java.util.List;

/**
 * P0 — Résultat d'un envoi multicast de notifications push.
 *
 * <p>Ce que contient ce résultat est ce qui permet à l'application de ne pas
 * mentir : le nombre d'appareils réellement servis, le nombre d'échecs, et
 * surtout la liste des tokens que le fournisseur a déclarés <b>invalides</b>.</p>
 *
 * <p>Un token invalide doit être <b>élagué</b> de la base : sans cela, il est
 * réessayé à chaque événement et la boucle ne s'arrête jamais.</p>
 *
 * @param success       {@code true} si aucun envoi n'a échoué
 * @param sentCount     nombre d'appareils servis
 * @param failedCount   nombre d'appareils en échec (hors tokens invalides comptés à part)
 * @param invalidTokens tokens désormais inutilisables, à élaguer
 */
public record PushResult(boolean success, int sentCount, int failedCount, List<String> invalidTokens) {

    public PushResult {
        invalidTokens = invalidTokens == null ? List.of() : List.copyOf(invalidTokens);
    }

    /** Aucun appareil ciblé : rien à faire, ce n'est pas un échec. */
    public static PushResult nothingToSend() {
        return new PushResult(true, 0, 0, List.of());
    }

    /**
     * Mode simulation ({@code app.push.dry-run=true}) : la charge utile a été
     * journalisée, aucun appel réseau n'a été fait. Le résultat est donc
     * « réussi » du point de vue du pipeline, mais avec zéro envoi réel.
     */
    public static PushResult dryRun() {
        return new PushResult(true, 0, 0, List.of());
    }

    /** Passerelle inactive ou non configurée : rien n'a été envoyé, et c'est assumé. */
    public static PushResult notSent() {
        return new PushResult(false, 0, 0, List.of());
    }
}
