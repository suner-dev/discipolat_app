package com.discipolat.modules.whatsapp.domain;

import com.discipolat.common.infrastructure.security.CryptoService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

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
    private final SoulRepository soulRepository;
    private final RestClient restClient;
    private final CryptoService cryptoService;

    @Value("${app.whatsapp.enabled:false}")
    private boolean globalEnabled;

    public WhatsAppService(WhatsAppConfigRepository configRepository,
                           WhatsAppMessageRepository messageRepository,
                           SoulRepository soulRepository,
                           CryptoService cryptoService) {
        this.configRepository = configRepository;
        this.messageRepository = messageRepository;
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
        if (body.startsWith("#rejoindre")) {
            sendText(tenantId, inbound.getPhoneNumber(),
                    cfg != null && cfg.getWelcomeMessage() != null ? cfg.getWelcomeMessage()
                    : "Bienvenue ! Vous êtes inscrit(e) aux annonces. #stop pour vous désabonner.",
                    null, null, WhatsAppMessage.Kind.COMMAND);
        } else if (body.startsWith("#stop") || body.startsWith("#arreter")) {
            sendText(tenantId, inbound.getPhoneNumber(),
                    "Vous ne recevrez plus d'annonces. #rejoindre pour revenir.",
                    null, null, WhatsAppMessage.Kind.COMMAND);
        } else if (body.startsWith("#aide")) {
            sendText(tenantId, inbound.getPhoneNumber(),
                    "Commandes :\n#rejoindre — recevoir les annonces\n#stop — se désabonner\n#aide — cette aide",
                    null, null, WhatsAppMessage.Kind.COMMAND);
        }
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
