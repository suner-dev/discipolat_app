package com.discipolat.modules.payments.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service de gestion des logs webhook — persistence et requêtage.
 */
@Service
@Transactional
public class WebhookLogService {

    private static final Logger log = LoggerFactory.getLogger(WebhookLogService.class);

    private final WebhookLogRepository repository;
    private final SecurityUtils securityUtils;

    public WebhookLogService(WebhookLogRepository repository, SecurityUtils securityUtils) {
        this.repository = repository;
        this.securityUtils = securityUtils;
    }

    /**
     * Enregistre un callback webhook reçu.
     */
    public WebhookLog logReceived(String provider, String endpoint, String sourceIp,
                                   String requestBody, Map<String, String> headers) {
        WebhookLog webhookLog = WebhookLog.builder()
                .tenantId(securityUtils.getCurrentTenantId())
                .provider(provider)
                .endpoint(endpoint)
                .sourceIp(sourceIp)
                .statusLabel("RECEIVED")
                .requestBody(truncate(requestBody, 10000))
                .requestHeaders(sanitizeHeaders(headers))
                .build();
        return repository.save(webhookLog);
    }

    /**
     * Met à jour un log après vérification HMAC.
     */
    public void markVerified(WebhookLog webhookLog, boolean valid, int durationMs) {
        webhookLog.setSignatureValid(valid);
        webhookLog.setStatusLabel(valid ? "VERIFIED" : "REJECTED");
        webhookLog.setDurationMs(durationMs);
        webhookLog.setStatusCode(valid ? 200 : 403);
        if (!valid) {
            webhookLog.setErrorMessage("HMAC signature verification failed");
        }
        repository.save(webhookLog);
    }

    /**
     * Met à jour un log après traitement complet.
     */
    public void markProcessed(WebhookLog webhookLog, String reference, UUID paymentId,
                               int statusCode, String responseBody, int durationMs) {
        webhookLog.setReference(reference);
        webhookLog.setPaymentId(paymentId);
        webhookLog.setStatusCode(statusCode);
        webhookLog.setResponseBody(truncate(responseBody, 5000));
        webhookLog.setDurationMs(durationMs);
        webhookLog.setStatusLabel("PROCESSED");
        repository.save(webhookLog);
    }

    /**
     * Met à jour un log en cas d'erreur.
     */
    public void markError(WebhookLog webhookLog, String errorMessage, int statusCode, int durationMs) {
        webhookLog.setErrorMessage(errorMessage);
        webhookLog.setStatusCode(statusCode);
        webhookLog.setDurationMs(durationMs);
        webhookLog.setStatusLabel("ERROR");
        repository.save(webhookLog);
    }

    // ── Requêtes ──

    @Transactional(readOnly = true)
    public Page<WebhookLog> search(String provider, String status, int page, int size) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return repository.search(tenantId, provider, status, LocalDateTime.now().minusDays(90),
                PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long total = repository.count();
        long received = repository.countByTenantIdAndStatusLabel(tenantId, "RECEIVED");
        long verified = repository.countByTenantIdAndStatusLabel(tenantId, "VERIFIED");
        long processed = repository.countByTenantIdAndStatusLabel(tenantId, "PROCESSED");
        long rejected = repository.countByTenantIdAndStatusLabel(tenantId, "REJECTED");
        long errors = repository.countByTenantIdAndStatusLabel(tenantId, "ERROR");

        Object[] byProvider = repository.statsByProvider(tenantId, since);

        return Map.of(
                "total", total,
                "received", received,
                "verified", verified,
                "processed", processed,
                "rejected", rejected,
                "errors", errors,
                "byProvider", byProvider != null ? byProvider : new Object[0]);
    }

    // ── Utilitaires ──

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) + "…[truncated]" : s;
    }

    /** Sanitize headers pour le stockage (supprime Authorization, Cookie, etc.). */
    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null) return null;
        return headers.entrySet().stream()
                .filter(e -> !e.getKey().toLowerCase().startsWith("authorization")
                        && !e.getKey().toLowerCase().startsWith("cookie")
                        && !e.getKey().toLowerCase().startsWith("x-webhook-secret")
                        && !e.getKey().toLowerCase().contains("password")
                        && !e.getKey().toLowerCase().contains("secret"))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey, e -> e.getValue() != null && e.getValue().length() > 200
                                ? e.getValue().substring(0, 200) + "…" : e.getValue()));
    }
}
