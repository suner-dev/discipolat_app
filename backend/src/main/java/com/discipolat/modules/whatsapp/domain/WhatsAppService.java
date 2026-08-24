package com.discipolat.modules.whatsapp.domain;

import com.discipolat.common.infrastructure.security.CryptoService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pont WhatsApp ↔ Discipolat — WhatsApp Business Cloud API (Meta).
 * Envoi de messages, réception webhooks, diffusion d'annonces, rappels.
 */
@Service
@Transactional
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String GRAPH_API = "https://graph.facebook.com/v20.0";

    private final WhatsAppConfigRepository configRepository;
    private final WhatsAppMessageRepository messageRepository;
    private final WhatsAppReminderRepository reminderRepository;
    private final SoulRepository soulRepository;
    private final RestClient restClient;
    private final CryptoService cryptoService;

    /** Abonnés WhatsApp par famille (numéro → set de families) */
    private final Map<String, Set<String>> familySubscribers = new ConcurrentHashMap<>();

    @Value("${app.whatsapp.enabled:false}")
    private boolean globalEnabled;

    public WhatsAppService(WhatsAppConfigRepository configRepository,
                           WhatsAppMessageRepository messageRepository,
                           WhatsAppReminderRepository reminderRepository,
                           SoulRepository soulRepository,
                           CryptoService cryptoService) {
        this.configRepository = configRepository;
        this.messageRepository = messageRepository;
        this.reminderRepository = reminderRepository;
        this.soulRepository = soulRepository;
        this.cryptoService = cryptoService;
        this.restClient = RestClient.create();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConfig(UUID tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Optional<WhatsAppConfig> cfg = configRepository.findByTenantId(tenantId);
        result.put("configured", cfg.isPresent());
        result.put("enabled", cfg.map(WhatsAppConfig::isEnabled).orElse(false));
        result.put("phoneNumberId", cfg.map(WhatsAppConfig::getPhoneNumberId).orElse(null));
        result.put("displayPhoneNumber", cfg.map(WhatsAppConfig::getDisplayPhoneNumber).orElse(null));
        result.put("welcomeMessage", cfg.map(WhatsAppConfig::getWelcomeMessage).orElse(null));
        result.put("hasToken", cfg.isPresent() && cfg.get().getAccessTokenEncrypted() != null);
        return result;
    }

    public WhatsAppConfig saveConfig(UUID tenantId, String phoneNumberId, String displayPhoneNumber,
                                     String accessToken, String webhookVerifyToken,
                                     boolean enabled, String welcomeMessage) {
        WhatsAppConfig cfg = configRepository.findByTenantId(tenantId).orElseGet(WhatsAppConfig::new);
        if (cfg.getTenantId() == null) { cfg.setTenantId(tenantId); cfg.setCreatedAt(LocalDateTime.now()); }
        if (phoneNumberId != null) cfg.setPhoneNumberId(phoneNumberId);
        if (displayPhoneNumber != null) cfg.setDisplayPhoneNumber(displayPhoneNumber);
        if (webhookVerifyToken != null) cfg.setWebhookVerifyToken(webhookVerifyToken);
        if (welcomeMessage != null) cfg.setWelcomeMessage(welcomeMessage);
        if (accessToken != null && !accessToken.isBlank()) cfg.setAccessTokenEncrypted(cryptoService.encrypt(accessToken));
        cfg.setEnabled(enabled && globalEnabled);
        return configRepository.save(cfg);
    }

    public Map<String, Object> testConnection(UUID tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        WhatsAppConfig cfg = requireConfig(tenantId);
        String token = cryptoService.decrypt(cfg.getAccessTokenEncrypted());
        try {
            var response = restClient.get()
                    .uri(GRAPH_API + "/" + cfg.getPhoneNumberId() + "?fields=display_phone_number,verified_name")
                    .header("Authorization", "Bearer " + token)
                    .retrieve().body(Map.class);
            result.put("success", true);
            result.put("details", response);
            cfg.setEnabled(true);
            configRepository.save(cfg);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public WhatsAppMessage sendText(UUID tenantId, String phoneNumber, String body,
                                    String referenceType, UUID referenceId, WhatsAppMessage.Kind kind) {
        WhatsAppConfig cfg = requireConfig(tenantId);
        WhatsAppMessage msg = new WhatsAppMessage();
        msg.setTenantId(tenantId);
        msg.setDirection(WhatsAppMessage.Direction.OUTBOUND);
        msg.setPhoneNumber(phoneNumber.replaceAll("[^0-9]", ""));
        msg.setBody(body);
        msg.setKind(kind == null ? WhatsAppMessage.Kind.TEXT : kind);
        msg.setReferenceType(referenceType);
        msg.setReferenceId(referenceId);

        String token = cryptoService.decrypt(cfg.getAccessTokenEncrypted());
        if (globalEnabled && token != null) {
            try {
                Map<String, Object> payload = Map.of(
                        "messaging_product", "whatsapp",
                        "to", msg.getPhoneNumber(),
                        "type", "text",
                        "text", Map.of("body", body));
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.post()
                        .uri(GRAPH_API + "/" + cfg.getPhoneNumberId() + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve().body(Map.class);
                msg.setStatus(WhatsAppMessage.Status.SENT);
                msg.setWaMessageId(extractWaId(response));
            } catch (Exception e) {
                log.warn("Envoi WhatsApp échoué vers {} : {}", msg.getPhoneNumber(), e.getMessage());
                msg.setStatus(WhatsAppMessage.Status.FAILED);
            }
        } else {
            msg.setStatus(WhatsAppMessage.Status.QUEUED);
        }
        return messageRepository.save(msg);
    }

    public Map<String, Object> broadcastAnnouncement(UUID tenantId, String titre, String contenu,
                                                     String referenceType, UUID referenceId) {
        List<Soul> recipients = soulRepository.findAll();
        int sent = 0, failed = 0;
        for (Soul s : recipients) {
            if (s.getTelephone() == null || s.getTelephone().isBlank()) continue;
            try {
                sendText(tenantId, s.getTelephone(),
                        "📢 *" + titre + "*\n\n" + contenu + "\n\n— Église Discipolat",
                        referenceType, referenceId, WhatsAppMessage.Kind.BROADCAST);
                sent++;
            } catch (Exception e) { failed++; }
        }
        return Map.of("recipients", recipients.size(), "sent", sent, "failed", failed);
    }

    public void handleWebhook(UUID tenantId, Map<String, Object> payload) {
        try {
            Object entryObj = payload.get("entry");
            if (!(entryObj instanceof List<?> entries)) return;
            for (Object e : entries) {
                if (!(e instanceof Map<?, ?> entry)) continue;
                Object changesObj = entry.get("changes");
                if (!(changesObj instanceof List<?> changes)) continue;
                for (Object c : changes) {
                    if (!(c instanceof Map<?, ?> change)) continue;
                    Object valueObj = change.get("value");
                    if (!(valueObj instanceof Map<?, ?> value)) continue;
                    handleIncomingMessages(tenantId, value);
                }
            }
        } catch (Exception ex) {
            log.error("Erreur webhook WhatsApp : {}", ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleIncomingMessages(UUID tenantId, Map<?, ?> value) {
        Object messagesObj = value.get("messages");
        if (!(messagesObj instanceof List<?> messages) || messages.isEmpty()) return;
        WhatsAppConfig cfg = configRepository.findByTenantId(tenantId).orElse(null);
        for (Object mObj : messages) {
            if (!(mObj instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) mObj;
            String from = String.valueOf(m.get("from"));
            String waId = String.valueOf(m.get("id"));
            String text = extractText(m);

            WhatsAppMessage inbound = new WhatsAppMessage();
            inbound.setTenantId(tenantId);
            inbound.setDirection(WhatsAppMessage.Direction.INBOUND);
            inbound.setPhoneNumber(from);
            inbound.setWaMessageId(waId);
            inbound.setBody(text);
            inbound.setStatus(WhatsAppMessage.Status.RECEIVED);
            messageRepository.save(inbound);

            // Process commands
            if (text != null && text.trim().toLowerCase().startsWith("#")) {
                processCommand(tenantId, cfg, inbound, text.trim().toLowerCase());
            }
        }
    }

    private void processCommand(UUID tenantId, WhatsAppConfig cfg, WhatsAppMessage inbound, String body) {
        String phone = inbound.getPhoneNumber();
        if (body.startsWith("#rejoindre famille")) {
            // P0 #1 — #rejoindre Famille-Nom : s'abonner aux annonces d'une famille
            String familyName = body.substring("#rejoindre famille".length()).trim();
            if (!familyName.isEmpty()) {
                familySubscribers.computeIfAbsent(phone, k -> new HashSet<>()).add(familyName.toUpperCase());
                sendText(tenantId, phone,
                        "✅ Vous êtes maintenant abonné(e) aux annonces de la famille '" + familyName.toUpperCase() + "'. " +
                        "Vous recevrez les rappels et annonces de cette famille.\n" +
                        "#rejoindre famille <nom> — ajouter une famille\n#quitter famille <nom> — se désabonner\n#afamille — voir vos familles",
                        null, null, WhatsAppMessage.Kind.COMMAND);
            } else {
                sendText(tenantId, phone,
                        "Usage : #rejoindre famille <nom-de-la-famille>\n" +
                        "Exemple : #rejoindre famille Grâce",
                        null, null, WhatsAppMessage.Kind.COMMAND);
            }
        } else if (body.startsWith("#quitter famille")) {
            String familyName = body.substring("#quitter famille".length()).trim();
            if (!familyName.isEmpty()) {
                Set<String> subs = familySubscribers.get(phone);
                if (subs != null) subs.remove(familyName.toUpperCase());
                sendText(tenantId, phone,
                        "❌ Vous ne recevrez plus les annonces de la famille '" + familyName.toUpperCase() + "'.",
                        null, null, WhatsAppMessage.Kind.COMMAND);
            }
        } else if (body.startsWith("#afamille")) {
            Set<String> subs = familySubscribers.getOrDefault(phone, Set.of());
            if (subs.isEmpty()) {
                sendText(tenantId, phone,
                        "Vous n'êtes abonné(e) à aucune famille.\n" +
                        "#rejoindre famille <nom> pour vous abonner.",
                        null, null, WhatsAppMessage.Kind.COMMAND);
            } else {
                sendText(tenantId, phone,
                        "📋 Vos familles abonnées :\n" + String.join(", ", subs),
                        null, null, WhatsAppMessage.Kind.COMMAND);
            }
        } else if (body.startsWith("#rejoindre")) {
            sendText(tenantId, phone,
                    cfg != null && cfg.getWelcomeMessage() != null ? cfg.getWelcomeMessage()
                    : "Bienvenue ! Vous êtes inscrit(e) aux annonces. #stop pour vous désabonner.\n"
                    + "#rejoindre famille <nom> — recevoir les annonces d'une famille",
                    null, null, WhatsAppMessage.Kind.COMMAND);
        } else if (body.startsWith("#stop") || body.startsWith("#arreter")) {
            sendText(tenantId, phone,
                    "Vous ne recevrez plus d'annonces. #rejoindre pour revenir.",
                    null, null, WhatsAppMessage.Kind.COMMAND);
        } else if (body.startsWith("#aide")) {
            sendText(tenantId, phone,
                    "Commandes :\n" +
                    "#rejoindre — recevoir les annonces générales\n" +
                    "#rejoindre famille <nom> — annonces d'une famille\n" +
                    "#quitter famille <nom> — se désabonner d'une famille\n" +
                    "#afamille — voir vos familles abonnées\n" +
                    "#stop — se désabonner de tout\n" +
                    "#aide — cette aide",
                    null, null, WhatsAppMessage.Kind.COMMAND);
        }
    }

    // ======================== P0 #1 — RAPPELS AUTOMATIQUES ========================

    /** Programme un rappel WhatsApp pour un événement ou suivi. */
    public WhatsAppReminder scheduleReminder(UUID tenantId, String referenceType, UUID referenceId,
                                              String phoneNumber, String message, LocalDateTime scheduledAt) {
        WhatsAppReminder reminder = new WhatsAppReminder();
        reminder.setTenantId(tenantId);
        reminder.setReferenceType(referenceType);
        reminder.setReferenceId(referenceId);
        reminder.setPhoneNumber(phoneNumber);
        reminder.setMessage(message);
        reminder.setScheduledAt(scheduledAt);
        return reminderRepository.save(reminder);
    }

    /** Envoie tous les rappels en attente dont l'heure est passée. Exécuté toutes les 5 min. */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void sendPendingReminders() {
        // Trouver tous les tenants qui ont des rappels en attente
        Set<UUID> tenants = new HashSet<>();
        reminderRepository.findAll().stream()
                .filter(r -> "PENDING".equals(r.getStatus()) && r.getScheduledAt().isBefore(LocalDateTime.now()))
                .forEach(r -> tenants.add(r.getTenantId()));

        for (UUID tenantId : tenants) {
            List<WhatsAppReminder> tenantReminders = reminderRepository
                    .findByTenantIdAndStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                            tenantId, "PENDING", LocalDateTime.now());
            for (WhatsAppReminder r : tenantReminders) {
                try {
                    sendText(tenantId, r.getPhoneNumber(), r.getMessage(),
                            r.getReferenceType(), r.getReferenceId(), WhatsAppMessage.Kind.REMINDER);
                    r.setStatus("SENT");
                    r.setSentAt(LocalDateTime.now());
                } catch (Exception e) {
                    r.setStatus("FAILED");
                    log.warn("Rappel WhatsApp échoué pour {} : {}", r.getPhoneNumber(), e.getMessage());
                }
                reminderRepository.save(r);
            }
        }
    }

    /** Rappel d'événement à envoyer 24h avant. */
    public void scheduleEventReminder(UUID tenantId, UUID eventId, String eventTitle,
                                       String phoneNumber, LocalDateTime eventDate) {
        LocalDateTime remindAt = eventDate.minusHours(24);
        if (remindAt.isBefore(LocalDateTime.now())) {
            remindAt = LocalDateTime.now().plusMinutes(5); // Fallback : dans 5 min
        }
        String msg = "🔔 Rappel : '" + eventTitle + "' demain à " +
                eventDate.getHour() + "h" + String.format("%02d", eventDate.getMinute()) + ". " +
                "Répondez GOING pour confirmer votre présence.";
        scheduleReminder(tenantId, "EVENT", eventId, phoneNumber, msg, remindAt);
    }

    private WhatsAppConfig requireConfig(UUID tenantId) {
        return configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("WhatsApp non configuré"));
    }

    private String extractText(Map<String, Object> message) {
        Object textObj = message.get("text");
        if (textObj instanceof Map<?, ?> t) {
            Object body = t.get("body");
            return body == null ? "" : String.valueOf(body);
        }
        return "[message non-textuel]";
    }

    @SuppressWarnings("unchecked")
    private String extractWaId(Map<String, Object> response) {
        if (response == null) return null;
        Object messages = response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) return String.valueOf(m.get("id"));
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<WhatsAppMessage> recentMessages(UUID tenantId, int limit) {
        return messageRepository.findByTenantIdOrderByCreatedAtDesc(tenantId,
                org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(UUID tenantId) {
        List<WhatsAppMessage> all = messageRepository.findByTenantIdOrderByCreatedAtDesc(tenantId,
                org.springframework.data.domain.PageRequest.of(0, 500)).getContent();
        long in = all.stream().filter(m -> m.getDirection() == WhatsAppMessage.Direction.INBOUND).count();
        long out = all.stream().filter(m -> m.getDirection() == WhatsAppMessage.Direction.OUTBOUND).count();
        long delivered = all.stream().filter(m -> m.getStatus() == WhatsAppMessage.Status.DELIVERED
                || m.getStatus() == WhatsAppMessage.Status.READ).count();
        return Map.of("inbound", in, "outbound", out, "deliveredOrRead", delivered);
    }
}
