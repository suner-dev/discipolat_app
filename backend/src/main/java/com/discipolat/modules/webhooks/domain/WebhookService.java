package com.discipolat.modules.webhooks.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/**
 * Connecteur Écosystème — webhooks sortants signés HMAC-SHA256 + clés API publiques.
 *
 * - {@link #fire(String, Map)} : notifie tous les webhooks actifs écoutant l'événement
 *   (payload JSON signé via en-tête X-Discipolat-Signature)
 * - Journal de livraison complet pour le debugging des intégrations
 * - Génération/révocation de clés API (hash SHA-256 stocké, clé visible une seule fois)
 */
@Service
@Transactional
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookRegistrationRepository webhookRepository;
    private final WebhookDeliveryLogRepository logRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WebhookService(WebhookRegistrationRepository webhookRepository,
                          WebhookDeliveryLogRepository logRepository,
                          ApiKeyRepository apiKeyRepository,
                          EntityPropagationPublisher propagationPublisher,
                          SecurityUtils securityUtils) {
        this.webhookRepository = webhookRepository;
        this.logRepository = logRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /* ------------------------------ Webhooks ------------------------------ */

    public WebhookRegistration create(WebhookRegistration registration) {
        registration.setTenantId(securityUtils.getCurrentTenantId());
        if (registration.getSecret() == null || registration.getSecret().isBlank()) {
            registration.setSecret(randomToken(32));
        }
        registration.setCreatedBy(securityUtils.getCurrentUserId());
        WebhookRegistration saved = webhookRepository.save(registration);
        propagationPublisher.publishCreated("WEBHOOK", saved.getId(),
                Map.of("name", saved.getName(), "url", saved.getUrl()),
                "Webhook créé: " + saved.getName());
        return saved;
    }

    public void delete(UUID id) {
        WebhookRegistration reg = findById(id);
        webhookRepository.delete(reg);
        propagationPublisher.publishDeleted("WEBHOOK", id,
                Map.of("name", reg.getName()), "Webhook supprimé: " + reg.getName());
    }

    @Transactional(readOnly = true)
    public List<WebhookRegistration> list() {
        return webhookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public WebhookRegistration findById(UUID id) {
        return webhookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WebhookRegistration", id));
    }

    /**
     * Déclenche un événement vers tous les webhooks abonnés.
     * Appelé par les autres modules (souls, finances, evangelism…).
     */
    public int fire(String eventType, Map<String, Object> payload) {
        List<WebhookRegistration> targets = list().stream()
                .filter(WebhookRegistration::isActive)
                .filter(w -> w.listensTo(eventType))
                .toList();

        String body = toJson(eventType, payload);
        for (WebhookRegistration target : targets) {
            deliver(target, eventType, body);
        }
        return targets.size();
    }

    /** Test manuel d'un webhook depuis l'admin. */
    public WebhookDeliveryLog test(UUID id) {
        WebhookRegistration reg = findById(id);
        return deliver(reg, "webhook.test",
                toJson("webhook.test", Map.of("message", "Test depuis Discipolat", "webhook", reg.getName())));
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryLog> deliveryLogs() {
        return logRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private WebhookDeliveryLog deliver(WebhookRegistration target, String eventType, String body) {
        String signature = hmacSha256(body, target.getSecret());
        int code = 0;
        boolean success = false;
        String error = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target.getUrl()))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("X-Discipolat-Event", eventType)
                    .header("X-Discipolat-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            code = response.statusCode();
            success = code >= 200 && code < 300;
        } catch (Exception e) {
            error = e.getMessage();
            log.debug("Webhook {} injoignable: {}", target.getName(), e.getMessage());
        }
        WebhookDeliveryLog entry = WebhookDeliveryLog.builder()
                .tenantId(target.getTenantId())
                .webhookId(target.getId())
                .eventType(eventType)
                .payload(body)
                .responseCode(code)
                .success(success)
                .errorMessage(error)
                .build();
        return logRepository.save(entry);
    }

    /* ------------------------------ Clés API ------------------------------ */

    /** Génère une clé API ; la valeur brute est retournée UNE SEULE fois. */
    public Map<String, Object> createApiKey(String name, String scopes) {
        String rawKey = "dk_" + randomToken(32);
        ApiKey apiKey = ApiKey.builder()
                .tenantId(securityUtils.getCurrentTenantId())
                .name(name)
                .keyHash(sha256(rawKey))
                .prefix(rawKey.substring(0, 11))
                .scopes(scopes != null && !scopes.isBlank() ? scopes : "read")
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        ApiKey saved = apiKeyRepository.save(apiKey);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getName());
        result.put("prefix", saved.getPrefix());
        result.put("scopes", saved.getScopes());
        result.put("key", rawKey); // visible une seule fois !
        result.put("warning", "Conservez cette clé précieusement — elle ne sera plus jamais affichée.");
        propagationPublisher.publishCreated("API_KEY", saved.getId(),
                Map.of("name", saved.getName(), "prefix", saved.getPrefix()),
                "Clé API créée: " + saved.getName());
        return result;
    }

    public void revokeApiKey(UUID id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ApiKey", id));
        key.setActive(false);
        apiKeyRepository.save(key);
        propagationPublisher.publishStatusChanged("API_KEY", id, "active", "revoked",
                "Clé API révoquée: " + key.getName());
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listApiKeys() {
        return apiKeyRepository.findAll();
    }

    /* ------------------------------ Utilitaires ------------------------------ */

    public static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC indisponible", e);
        }
    }

    public static String sha256(String data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        return sb.toString();
    }

    private static String randomToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    /** Sérialisation JSON minimale sans dépendance additionnelle. */
    private static String toJson(String eventType, Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"event\":\"").append(escape(eventType)).append("\",");
        sb.append("\"timestamp\":\"").append(java.time.Instant.now()).append("\",");
        sb.append("\"data\":{");
        boolean first = true;
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(escape(v.toString())).append("\"");
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
