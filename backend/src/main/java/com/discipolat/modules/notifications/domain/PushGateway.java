package com.discipolat.modules.notifications.domain;

import java.util.List;

/**
 * P0 — Point de sortie des notifications push vers les appareils mobiles.
 *
 * <p>Ce contrat existe pour une raison d'honnêteté : avant lui,
 * {@code PushTokenController} enregistrait des tokens FCM et <b>aucun</b>
 * code ne les utilisait. L'application mobile embarquant
 * {@code firebase_messaging} ne recevait donc jamais rien. En passant par une
 * interface, l'absence d'envoi devient un état explicite
 * ({@link NoOpPushGateway}) et non un silence.</p>
 *
 * <p>Implémentations :</p>
 * <ul>
 *   <li>{@link FirebaseAdminPushGateway} — envoi réel via {@code firebase-admin} ;</li>
 *   <li>{@link NoOpPushGateway} — passelle active par défaut, journalise l'abandon.</li>
 * </ul>
 */
public interface PushGateway {

    /**
     * Envoie un même message à plusieurs appareils.
     *
     * <p><b>Ne doit jamais lever d'exception</b> pour un échec unitaire : un
     * token mort ne doit pas faire échouer le multicast entier. Les erreurs
     * indivisibles (passerelle injoignable) sont converties en
     * {@link PushResult#notSent()}.</p>
     *
     * @param deviceTokens tokens FCM des appareils ciblés
     * @param message      contenu à diffuser
     * @return bilan de l'envoi, tokens invalides compris
     */
    PushResult send(List<String> deviceTokens, PushMessage message);
}
