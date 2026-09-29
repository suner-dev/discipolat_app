package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P0 — Orchestrateur de la diffusion push.
 *
 * <p>Chaîne complète, dans cet ordre :</p>
 * <ol>
 *   <li>vérifier que l'utilisateur n'a pas coupé le canal {@code PUSH}
 *       dans ses {@link NotificationPreference} ;</li>
 *   <li>charger ses tokens d'appareil enregistrés ;</li>
 *   <li>déléguer l'envoi à la {@link PushGateway} <b>par l'interface</b>
 *       (jamais au fournisseur directement) ;</li>
 *   <li><b>élaguer</b> les tokens déclarés invalides, pour qu'un appareil mort
 *       ne soit plus jamais ciblé.</li>
 * </ol>
 *
 * <p>Aucune de ces étapes ne lève d'exception : une notification push est un
 * confort, jamais une raison de faire échouer l'opération métier qui l'a
 * déclenchée.</p>
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushGateway pushGateway;
    private final PushTokenRepository pushTokenRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public PushNotificationService(PushGateway pushGateway,
                                   PushTokenRepository pushTokenRepository,
                                   NotificationPreferenceRepository notificationPreferenceRepository) {
        this.pushGateway = pushGateway;
        this.pushTokenRepository = pushTokenRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    /**
     * Enregistre (ou réaffecte) un token d'appareil. Un token déjà connu est
     * simplement rattaché au nouvel utilisateur : il n'y a qu'un compte par
     * appareil.
     */
    @Transactional
    public void registerToken(UUID tenantId, UUID userId, String token, String platform, String appVersion) {
        if (token == null || token.isBlank()) {
            log.warn("[Push] Enregistrement refusé : token vide (user={})", userId);
            return;
        }
        PushToken entity = pushTokenRepository.findByToken(token).orElseGet(PushToken::new);
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setToken(token);
        if (platform != null && !platform.isBlank()) {
            entity.setPlatform(platform);
        }
        if (appVersion != null && !appVersion.isBlank()) {
            entity.setAppVersion(appVersion);
        }
        pushTokenRepository.save(entity);
        log.info("[Push] Token enregistré : user={}, platform={}", userId, entity.getPlatform());
    }

    /** Retire un token du ciblage (désinstallation, déconnexion). */
    @Transactional
    public void unregisterToken(UUID tenantId, UUID userId, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        PushToken existing = pushTokenRepository.findByToken(token).orElse(null);
        if (existing == null) {
            return;
        }
        if (!existing.getUserId().equals(userId)) {
            log.warn("[Push] Désenregistrement refusé : token {} appartient à un autre utilisateur", userId);
            return;
        }
        pushTokenRepository.delete(existing);
        log.info("[Push] Token désenregistré : user={}", userId);
    }

    /**
     * L'utilisateur accepte-t-il le canal {@code PUSH} ?
     * Sans préférence enregistrée, le canal est accepté (opt-out, pas opt-in).
     */
    @Transactional(readOnly = true)
    public boolean isPushAllowed(UUID userId) {
        try {
            return notificationPreferenceRepository.findByUserId(userId)
                    .map(pref -> pref.allows(CanalNotification.PUSH))
                    .orElse(Boolean.TRUE);
        } catch (Exception e) {
            log.warn("[Push] Préférences illisibles pour {} : {} — push autorisé par prudence", userId, e.getMessage());
            return true;
        }
    }

    /**
     * Diffuse un message aux appareils d'un utilisateur.
     *
     * @return le bilan de l'envoi, tokens invalides compris
     */
    public PushResult pushToUser(UUID tenantId, UUID userId, String titre, String message, Map<String, String> data) {
        if (userId == null) {
            return PushResult.nothingToSend();
        }
        if (!isPushAllowed(userId)) {
            log.debug("[Push] Canal PUSH refusé par les préférences de {}", userId);
            return PushResult.nothingToSend();
        }
        List<String> tokens = activeTokens(userId);
        if (tokens.isEmpty()) {
            log.debug("[Push] Aucun appareil enregistré pour {}", userId);
            return PushResult.nothingToSend();
        }

        PushResult result;
        try {
            result = pushGateway.send(tokens, new PushMessage(titre, message, data));
        } catch (Exception e) {
            // Défense : le contrat de PushGateway interdit de lever, mais une
            // implémentation tierce ne doit pas casser l'appelant.
            log.error("[Push] Passelle push en échec pour {} : {}", userId, e.getMessage(), e);
            return PushResult.notSent();
        }
        reapInvalidTokens(tenantId, userId, result);
        return result;
    }

    private List<String> activeTokens(UUID userId) {
        try {
            return pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .map(PushToken::getToken)
                    .filter(t -> t != null && !t.isBlank())
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("[Push] Tokens illisibles pour {} : {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Élagage des tokens morts. Sans cette étape, un token révoqué est
     * réessayé à chaque événement et la boucle est infinie.
     */
    private void reapInvalidTokens(UUID tenantId, UUID userId, PushResult result) {
        List<String> invalid = result.invalidTokens();
        if (invalid.isEmpty()) {
            return;
        }
        if (tenantId == null) {
            log.warn("[Push] {} token(s) invalide(s) non élagués : tenant inconnu (event {})", invalid.size(), userId);
            return;
        }
        try {
            int deleted = pushTokenRepository.deleteInvalidTokens(tenantId, invalid);
            log.info("[Push] {} token(s) invalide(s) élagué(s) pour {}", deleted, userId);
        } catch (Exception e) {
            log.warn("[Push] Élagage impossible pour {} : {}", userId, e.getMessage());
        }
    }
}
