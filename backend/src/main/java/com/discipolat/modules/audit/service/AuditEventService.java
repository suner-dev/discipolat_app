package com.discipolat.modules.audit.service;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditEvent;
import com.discipolat.modules.audit.domain.BusinessHistory;
import com.discipolat.modules.audit.repository.AuditEventRepository;
import com.discipolat.modules.audit.repository.BusinessHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final BusinessHistoryRepository businessHistoryRepository;
    private final SecurityUtils securityUtils;

    public AuditEventService(AuditEventRepository auditEventRepository,
                             BusinessHistoryRepository businessHistoryRepository,
                             SecurityUtils securityUtils) {
        this.auditEventRepository = auditEventRepository;
        this.businessHistoryRepository = businessHistoryRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Enregistre un événement d'audit technique (qui a fait quoi).
     * La chaîne de hachage est calculée ET stockée dans la même ligne (prev_hash + hash).
     * Cette méthode est appelée depuis l'outbox consumer ou directement par les services métier.
     */
    public AuditEvent log(UUID tenantId, UUID actorId, String actorEmail, String action, String entity,
                          UUID entityId, Map<String, Object> oldValue, Map<String, Object> newValue,
                          String ip, String userAgent) {
        String prevHash = getLatestHash(tenantId);
        String hash = computeHash(prevHash, tenantId, actorId, action, entity, entityId,
                oldValue, newValue, ip, userAgent, OffsetDateTime.now());

        AuditEvent event = AuditEvent.builder()
                .tenantId(tenantId)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .oldValueJson(oldValue)
                .newValueJson(newValue)
                .ip(ip)
                .userAgent(userAgent)
                .prevHash(prevHash)
                .hash(hash)
                .timestamp(OffsetDateTime.now())
                .build();

        return auditEventRepository.save(event);
    }

    /**
     * Version simplifiée pour logging rapide (sans ancienne/nouvelle valeur).
     */
    public AuditEvent logSimple(UUID tenantId, UUID actorId, String action, String entity, UUID entityId) {
        return log(tenantId, actorId, null, action, entity, entityId, Map.of(), Map.of(), null, null);
    }

    /**
     * Récupère le hash du dernier événement d'audit pour ce tenant (pour chaînage).
     */
    private String getLatestHash(UUID tenantId) {
        return auditEventRepository.findFirstByTenantIdOrderByTimestampDesc(tenantId)
                .map(AuditEvent::getHash)
                .orElse("GENESIS");
    }

    /**
     * Calcule le SHA-256 de l'événement pour la chaîne d'intégrité.
     * Format: prev_hash || tenant_id || actor_id || action || entity || entity_id || old_value || new_value || ip || user_agent || timestamp
     */
    private String computeHash(String prevHash, UUID tenantId, UUID actorId, String action, String entity,
                               UUID entityId, Map<String, Object> oldValue, Map<String, Object> newValue,
                               String ip, String userAgent, OffsetDateTime timestamp) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(prevHash).append('|')
                    .append(tenantId).append('|')
                    .append(actorId != null ? actorId : "").append('|')
                    .append(action).append('|')
                    .append(entity).append('|')
                    .append(entityId != null ? entityId : "").append('|')
                    .append(oldValue != null ? oldValue.toString() : "").append('|')
                    .append(newValue != null ? newValue.toString() : "").append('|')
                    .append(ip != null ? ip : "").append('|')
                    .append(userAgent != null ? userAgent : "").append('|')
                    .append(timestamp.toInstant().toEpochMilli());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /**
     * Vérifie l'intégrité complète de la chaîne d'audit pour un tenant.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyAuditChain(UUID tenantId) {
        List<AuditEvent> events = auditEventRepository.findByTenantIdOrderByTimestampDesc(tenantId);
        boolean valid = true;
        String expectedPrevious = "GENESIS";
        int checked = 0;

        // On vérifie dans l'ordre chronologique (plus ancien -> plus récent)
        for (int i = events.size() - 1; i >= 0; i--) {
            AuditEvent event = events.get(i);
            if (!expectedPrevious.equals(event.getPrevHash())) {
                valid = false;
                break;
            }
            expectedPrevious = event.getHash();
            checked++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", valid);
        result.put("checked", checked);
        result.put("totalEvents", events.size());
        result.put("headHash", events.isEmpty() ? null : events.get(0).getHash());
        return result;
    }

    /**
     * Recherche paginée des événements d'audit (pour API admin).
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findFiltered(UUID tenantId, UUID actorId, String entity, String action,
                                         OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        if (actorId != null) {
            return auditEventRepository.findByTenantIdAndActorIdOrderByTimestampDesc(tenantId, actorId, pageable);
        }
        if (from != null && to != null) {
            return auditEventRepository.findByTenantIdAndTimestampBetween(tenantId, from, to, pageable);
        }
        return auditEventRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
    }

    /**
     * Export CSV des événements d'audit (audité : qui exporte quoi).
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID tenantId, UUID actorId, String entity, String action,
                            OffsetDateTime from, OffsetDateTime to) {
        List<AuditEvent> events = auditEventRepository.findByTenantIdAndTimestampBetween(tenantId,
                from != null ? from : OffsetDateTime.now().minusYears(1),
                to != null ? to : OffsetDateTime.now());

        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8
        csv.append("Timestamp;Acteur;Email Acteur;Action;Entité;Entité ID;Valeur Précédente;Nouvelle Valeur;IP;User-Agent;Hash\n");

        for (AuditEvent e : events) {
            csv.append(csvField(e.getTimestamp().toString())).append(';')
                    .append(csvField(e.getActorId() != null ? e.getActorId().toString() : "Système")).append(';')
                    .append(csvField(e.getActorEmail())).append(';')
                    .append(csvField(e.getAction())).append(';')
                    .append(csvField(e.getEntity())).append(';')
                    .append(csvField(e.getEntityId() != null ? e.getEntityId().toString() : "")).append(';')
                    .append(csvField(e.getOldValueJson() != null ? e.getOldValueJson().toString() : "")).append(';')
                    .append(csvField(e.getNewValueJson() != null ? e.getNewValueJson().toString() : "")).append(';')
                    .append(csvField(e.getIp())).append(';')
                    .append(csvField(e.getUserAgent())).append(';')
                    .append(csvField(e.getHash())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Enregistre un événement d'historique métier (ce qui est arrivé à l'objet).
     * Distinct de l'audit technique : pas de hash chain, mais requêtable par objet métier.
     */
    public BusinessHistory recordBusinessHistory(UUID tenantId, String objectType, UUID objectId,
                                                 String eventType, String summary, Map<String, Object> detail,
                                                 UUID actorId, String actorRole, UUID spaceId) {
        BusinessHistory history = BusinessHistory.builder()
                .tenantId(tenantId)
                .objectType(objectType)
                .objectId(objectId)
                .eventType(eventType)
                .summary(summary)
                .detailJson(detail)
                .actorId(actorId)
                .actorRole(actorRole)
                .spaceId(spaceId)
                .happenedAt(OffsetDateTime.now())
                .build();

        return businessHistoryRepository.save(history);
    }

    /**
     * Récupère l'historique métier d'un objet (ex: timeline d'un asset, d'un événement, etc.)
     */
    @Transactional(readOnly = true)
    public List<BusinessHistory> getObjectHistory(UUID tenantId, String objectType, UUID objectId) {
        return businessHistoryRepository.findByTenantIdAndObjectTypeAndObjectIdOrderByHappenedAtDesc(tenantId, objectType, objectId);
    }

    /**
     * Récupère l'historique métier d'un espace (pour dashboard espace).
     */
    @Transactional(readOnly = true)
    public List<BusinessHistory> getSpaceHistory(UUID tenantId, UUID spaceId) {
        return businessHistoryRepository.findByTenantIdAndSpaceIdOrderByHappenedAtDesc(tenantId, spaceId);
    }

    private String csvField(String value) {
        if (value == null) return "";
        String cleaned = value.replace("\r", " ").replace("\n", " ");
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }
}