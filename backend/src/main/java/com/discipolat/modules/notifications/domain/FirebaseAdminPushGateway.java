package com.discipolat.modules.notifications.domain;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * P0 — Passelle push réelle via {@code com.google.firebase:firebase-admin}.
 *
 * <p>Règles d'honnêteté appliquées :</p>
 * <ul>
 *   <li><b>Aucun secret en configuration</b> : l'initialisation lit un
 *       <i>fichier</i> de compte de service dont le chemin vient de
 *       {@code app.push.credentials-path}. Le JSON n'est jamais lu depuis une
 *       variable d'environnement et n'est jamais journalisé.</li>
 *   <li><b>Mode simulation par défaut</b> ({@code app.push.dry-run=true}) :
 *       la charge utile est journalisée et <b>aucun appel réseau</b> n'est
 *       fait. Aucun identifiant Firebase n'existe pour l'instant : sans cette
 *       bascule, la feature mentirait à nouveau.</li>
 *   <li><b>Jamais d'exception sur un token mort</b> : un
 *       {@code UNREGISTERED} est collecté dans {@link PushResult} pour être
 *       élagué, sans réessai.</li>
 *   <li><b>Initialisation paresseuse</b> : sans jeton de compte de service, la
 *       classe s'instancie normalement (pas de plantage au démarrage) et
 *       l'initialisation n'a lieu qu'au premier envoi réel.</li>
 * </ul>
 */
public class FirebaseAdminPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminPushGateway.class);

    /** FCM limite un multicast à 500 tokens par requête. */
    public static final int MAX_TOKENS_PER_MULTICAST = 500;

    /** Nom de l'instance Firebase dédiée : n'écrase pas l'instance par défaut. */
    public static final String FIREBASE_APP_NAME = "discipolat-push";

    /** Marqueur d'erreur legacy renvoyé par FCM pour un token révoqué. */
    static final String LEGACY_INVALID_TOKEN_MARKER = "registration-token-not-registered";

    private final PushProperties properties;
    private final Object initLock = new Object();

    private volatile FirebaseMessaging messaging;

    public FirebaseAdminPushGateway(PushProperties properties) {
        this.properties = properties;
    }

    @Override
    public PushResult send(List<String> deviceTokens, PushMessage message) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return PushResult.nothingToSend();
        }

        if (properties.isDryRun()) {
            log.info("[Push:dry-run] {} appareil(s) non contacté(s) — « {} » : {} | data={}",
                    deviceTokens.size(), message.title(), message.body(), message.data());
            return PushResult.dryRun();
        }

        if (!properties.isConfigured()) {
            log.warn("[Push] Envoi impossible, Firebase n'est pas configuré : {}", properties.reason());
            return PushResult.notSent();
        }

        try {
            FirebaseMessaging client = messaging();
            return deliver(client, deviceTokens, message);
        } catch (RuntimeException e) {
            // Filet de sécurité : une anomalie inattendue ne doit jamais
            // remonter jusqu'à l'appelant (ni au consommateur outbox).
            log.error("[Push] Échec inattendu de l'envoi push : {}", e.getMessage(), e);
            return PushResult.notSent();
        }
    }

    /**
     * Multicast par lots de {@link #MAX_TOKENS_PER_MULTICAST}. Un lot rejeté
     * globalement (authentification, quota, réseau) est compté en échec mais
     * <b>neutralise</b> : il n'interrompt pas les lots suivants et n'élague
     * aucun token, puisque l'anomalie n'est pas propre à un appareil.
     */
    private PushResult deliver(FirebaseMessaging client, List<String> deviceTokens, PushMessage message) {
        int sent = 0;
        int failed = 0;
        Set<String> invalid = new LinkedHashSet<>();

        for (List<String> batch : batches(deviceTokens)) {
            BatchResponse response;
            try {
                response = sendBatch(client, batch, message);
            } catch (PushGatewayException e) {
                failed += batch.size();
                continue;
            }
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < batch.size(); i++) {
                String token = batch.get(i);
                SendResponse sendResponse = i < responses.size() ? responses.get(i) : null;
                if (sendResponse != null && sendResponse.isSuccessful()) {
                    sent++;
                    continue;
                }
                failed++;
                FirebaseMessagingException failure = sendResponse != null ? sendResponse.getException() : null;
                if (isInvalidToken(failure)) {
                    invalid.add(token);
                } else {
                    log.warn("[Push] Envoi refusé pour un appareil : {}",
                            failure != null ? failure.getMessage() : "réponse absente du lot");
                }
            }
        }

        if (!invalid.isEmpty()) {
            log.info("[Push] {} token(s) désormais invalides, à élaguer de la base", invalid.size());
        }
        return new PushResult(failed == 0, sent, failed, List.copyOf(invalid));
    }

    private BatchResponse sendBatch(FirebaseMessaging client, List<String> batch, PushMessage message) {
        try {
            return client.sendEachForMulticast(multicastBuilder(batch, message).build());
        } catch (FirebaseMessagingException e) {
            // Échec global du lot (authentification, quota global, réseau) :
            // aucun token individuel n'est en cause, donc AUCUN n'est élagué.
            log.error("[Push] Lot de {} token(s) rejeté par FCM : {}", batch.size(), e.getMessage());
            throw new PushGatewayException("Lot de " + batch.size() + " token(s) rejeté par FCM", e);
        }
    }

    /** Construction du message FCM — isolée pour être vérifiable sans réseau. */
    static MulticastMessage.Builder multicastBuilder(List<String> tokens, PushMessage message) {
        MulticastMessage.Builder builder = MulticastMessage.builder().addAllTokens(tokens);
        if (!message.isEmpty()) {
            builder.setNotification(Notification.builder()
                    .setTitle(message.title() == null ? "" : message.title())
                    .setBody(message.body() == null ? "" : message.body())
                    .build());
        }
        if (!message.data().isEmpty()) {
            builder.putAllData(message.data());
        }
        return builder;
    }

    /**
     * Un token est mort — et doit donc être élagué — si FCM répond
     * {@code UNREGISTERED}, {@code INVALID_ARGUMENT}, ou si le message d'erreur
     * contient le marqueur historique {@code registration-token-not-registered}.
     * Aucun réessai n'est tenté sur ces erreurs.
     */
    static boolean isInvalidToken(FirebaseMessagingException exception) {
        if (exception == null) {
            return false;
        }
        MessagingErrorCode code = exception.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            return true;
        }
        String message = exception.getMessage();
        return message != null
                && message.toLowerCase(Locale.ROOT).contains(LEGACY_INVALID_TOKEN_MARKER);
    }

    /** Découpe une liste de tokens en lots de {@link #MAX_TOKENS_PER_MULTICAST}. */
    static List<List<String>> batches(List<String> tokens) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += MAX_TOKENS_PER_MULTICAST) {
            out.add(List.copyOf(tokens.subList(i, Math.min(i + MAX_TOKENS_PER_MULTICAST, tokens.size()))));
        }
        return out;
    }

    /**
     * Instance Firebase créée à la demande et réutilisée ensuite.
     *
     * @throws PushGatewayException si le fichier de compte de service est
     *                              illisible ou invalide
     */
    private FirebaseMessaging messaging() {
        FirebaseMessaging local = messaging;
        if (local != null) {
            return local;
        }
        synchronized (initLock) {
            if (messaging == null) {
                messaging = FirebaseMessaging.getInstance(firebaseApp());
            }
            return messaging;
        }
    }

    private FirebaseApp firebaseApp() {
        Path credentials = properties.credentialsFile();
        if (credentials == null) {
            throw new PushGatewayException("Fichier de compte de service introuvable : " + properties.reason(), null);
        }
        try (InputStream in = Files.newInputStream(credentials)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            try {
                return FirebaseApp.getInstance(FIREBASE_APP_NAME);
            } catch (IllegalStateException notYetInitialised) {
                return FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
            }
        } catch (IOException e) {
            // Le chemin est journalisé, jamais le contenu du fichier.
            throw new PushGatewayException("Compte de service Firebase illisible : " + credentials, e);
        }
    }

    /** Erreur technique d'envoi, déjà journalisée : ne doit pas remonter à l'appelant. */
    static class PushGatewayException extends RuntimeException {
        PushGatewayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
