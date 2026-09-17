package com.discipolat.modules.core.service;

import com.discipolat.modules.audit.service.AuditEventService;
import com.discipolat.modules.core.domain.OutboxEvent;
import com.discipolat.modules.core.domain.ProcessedEvent;
import com.discipolat.modules.core.repository.OutboxEventRepository;
import com.discipolat.modules.core.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxConsumers {

    private final OutboxPublisher outboxPublisher;
    private final AuditEventService auditEventService;
    private final RealTimeService realTimeService;
    private final OutboxEventRepository outboxRepository;
    private final ProcessedEventRepository processedRepository;

    /**
     * Initialise tous les consommateurs canoniques (Annexe E).
     * Appelé au démarrage de l'application.
     */
    @Transactional
    public void registerAllConsumers() {
        // NOTIFY - Notifications in-app, push, email
        outboxPublisher.registerConsumer("AssetCheckedOut", this::consumeNotify);
        outboxPublisher.registerConsumer("AssetReturned", this::consumeNotify);
        outboxPublisher.registerConsumer("AssetDamaged", this::consumeNotify);
        outboxPublisher.registerConsumer("MaintenanceStarted", this::consumeNotify);
        outboxPublisher.registerConsumer("MaintenanceCompleted", this::consumeNotify);
        outboxPublisher.registerConsumer("ExpenseCreated", this::consumeNotify);
        outboxPublisher.registerConsumer("MemberRegistered", this::consumeNotify);
        outboxPublisher.registerConsumer("MemberTransferred", this::consumeNotify);
        outboxPublisher.registerConsumer("RoleAssigned", this::consumeNotify);
        outboxPublisher.registerConsumer("RoleEnded", this::consumeNotify);
        outboxPublisher.registerConsumer("PastorAppointed", this::consumeNotify);
        outboxPublisher.registerConsumer("EventCreated", this::consumeNotify);
        outboxPublisher.registerConsumer("TaskAssigned", this::consumeNotify);
        outboxPublisher.registerConsumer("TaskCompleted", this::consumeNotify);
        outboxPublisher.registerConsumer("AttendanceRecorded", this::consumeNotify);
        outboxPublisher.registerConsumer("PrayerSessionCompleted", this::consumeNotify);
        outboxPublisher.registerConsumer("SermonPublished", this::consumeNotify);
        outboxPublisher.registerConsumer("DressCodePublished", this::consumeNotify);
        outboxPublisher.registerConsumer("SpaceConfigChanged", this::consumeNotify);
        outboxPublisher.registerConsumer("StatusChanged", this::consumeNotify);
        outboxPublisher.registerConsumer("InvitationAccepted", this::consumeNotify);
        outboxPublisher.registerConsumer("PermissionsChanged", this::consumeNotify);
        outboxPublisher.registerConsumer("JourneyStageChanged", this::consumeNotify);
        outboxPublisher.registerConsumer("MentorAssigned", this::consumeNotify);
        outboxPublisher.registerConsumer("HealthConsultationCreated", this::consumeNotify);
        outboxPublisher.registerConsumer("PrescriptionIssued", this::consumeNotify);
        outboxPublisher.registerConsumer("CampaignStarted", this::consumeNotify);
        outboxPublisher.registerConsumer("KitDistributed", this::consumeNotify);
        outboxPublisher.registerConsumer("StockLowAlert", this::consumeNotify);
        outboxPublisher.registerConsumer("MedicineExpiring", this::consumeNotify);
        outboxPublisher.registerConsumer("HealthReferralCreated", this::consumeNotify);

        // AUDIT - Journal d'audit technique (hash chain)
        outboxPublisher.registerConsumer("AssetCheckedOut", this::consumeAudit);
        outboxPublisher.registerConsumer("AssetReturned", this::consumeAudit);
        outboxPublisher.registerConsumer("AssetDamaged", this::consumeAudit);
        outboxPublisher.registerConsumer("ExpenseCreated", this::consumeAudit);
        outboxPublisher.registerConsumer("MemberRegistered", this::consumeAudit);
        outboxPublisher.registerConsumer("MemberTransferred", this::consumeAudit);
        outboxPublisher.registerConsumer("RoleAssigned", this::consumeAudit);
        outboxPublisher.registerConsumer("RoleEnded", this::consumeAudit);
        outboxPublisher.registerConsumer("PastorAppointed", this::consumeAudit);
        outboxPublisher.registerConsumer("EventCreated", this::consumeAudit);
        outboxPublisher.registerConsumer("SpaceConfigChanged", this::consumeAudit);
        outboxPublisher.registerConsumer("StatusChanged", this::consumeAudit);
        outboxPublisher.registerConsumer("InvitationAccepted", this::consumeAudit);

        // BUSINESS_HISTORY - Historique métier par objet
        outboxPublisher.registerConsumer("AssetCheckedOut", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("AssetReturned", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("AssetDamaged", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("MaintenanceStarted", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("MaintenanceCompleted", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("ExpenseCreated", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("MemberTransferred", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("RoleAssigned", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("RoleEnded", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("PastorAppointed", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("EventCreated", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("TaskAssigned", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("TaskCompleted", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("AttendanceRecorded", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("SermonPublished", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("DressCodePublished", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("SpaceConfigChanged", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("StatusChanged", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("JourneyStageChanged", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("MentorAssigned", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("HealthConsultationCreated", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("PrescriptionIssued", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("CampaignStarted", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("KitDistributed", this::consumeBusinessHistory);
        outboxPublisher.registerConsumer("HealthReferralCreated", this::consumeBusinessHistory);

        // FINANCE - Auto-création dépenses (TCO, etc.)
        outboxPublisher.registerConsumer("AssetDamaged", this::consumeFinance);
        outboxPublisher.registerConsumer("MaintenanceCompleted", this::consumeFinance);
        outboxPublisher.registerConsumer("ExpenseCreated", this::consumeFinance);

        // ANALYTICS - Compteurs, métriques
        outboxPublisher.registerConsumer("MemberRegistered", this::consumeAnalytics);
        outboxPublisher.registerConsumer("EventCreated", this::consumeAnalytics);
        outboxPublisher.registerConsumer("AttendanceRecorded", this::consumeAnalytics);
        outboxPublisher.registerConsumer("PrayerSessionCompleted", this::consumeAnalytics);
        outboxPublisher.registerConsumer("SermonPublished", this::consumeAnalytics);
        outboxPublisher.registerConsumer("TaskCompleted", this::consumeAnalytics);

        // REALTIME - Push WebSocket clients
        outboxPublisher.registerConsumer("SpaceConfigChanged", this::consumeRealtime);
        outboxPublisher.registerConsumer("StatusChanged", this::consumeRealtime);
        outboxPublisher.registerConsumer("RoleAssigned", this::consumeRealtime);
        outboxPublisher.registerConsumer("MemberTransferred", this::consumeRealtime);
        outboxPublisher.registerConsumer("DressCodePublished", this::consumeRealtime);
        outboxPublisher.registerConsumer("InvitationAccepted", this::consumeRealtime);
        outboxPublisher.registerConsumer("TaskAssigned", this::consumeRealtime);
        outboxPublisher.registerConsumer("TaskCompleted", this::consumeRealtime);

        log.info("Outbox consumers registered: {} event types", 29);
    }

    // ========== Consumer implementations ==========

    private void consumeNotify(OutboxEvent event) {
        // TODO: Déléguer à NotificationService pour créer notifications in-app/push/email
        log.debug("NOTIFY consumer: {}", event.getEventType());
    }

    private void consumeAudit(OutboxEvent event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = event.getPayloadJson();
        UUID actorId = getUUID(payload, "actorId");
        String actorEmail = (String) payload.get("actorEmail");
        String action = event.getEventType();
        String entity = event.getAggregateType();
        UUID entityId = event.getAggregateId();
        Object oldValue = payload.get("oldValue");
        Object newValue = payload.get("newValue");
        String ip = (String) payload.get("ip");
        String userAgent = (String) payload.get("userAgent");

        auditEventService.log(event.getTenantId(), actorId, actorEmail, action, entity, entityId,
                oldValue != null ? Map.of("value", oldValue) : Map.of(),
                newValue != null ? Map.of("value", newValue) : Map.of(),
                ip, userAgent);
    }

    private void consumeBusinessHistory(OutboxEvent event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = event.getPayloadJson();
        String summary = buildSummary(event.getEventType(), payload);

        auditEventService.recordBusinessHistory(
                event.getTenantId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                summary,
                payload,
                getUUID(payload, "actorId"),
                (String) payload.get("actorRole"),
                getUUID(payload, "spaceId")
        );
    }

    private void consumeFinance(OutboxEvent event) {
        // TODO: Déléguer à FinanceService pour auto-création dépenses TCO, etc.
        log.debug("FINANCE consumer: {}", event.getEventType());
    }

    private void consumeAnalytics(OutboxEvent event) {
        // TODO: Déléguer à AnalyticsService pour incrémenter compteurs
        log.debug("ANALYTICS consumer: {}", event.getEventType());
    }

    private void consumeRealtime(OutboxEvent event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = event.getPayloadJson();

        switch (event.getEventType()) {
            case "SpaceConfigChanged" ->
                    realTimeService.pushSpaceConfigChanged(event.getTenantId(), event.getAggregateId(),
                            (String) payload.get("changeType"), payload);
            case "StatusChanged" ->
                    realTimeService.pushStatusChanged(event.getTenantId(), event.getAggregateType(),
                            event.getAggregateId(),
                            (String) payload.get("fromCode"), (String) payload.get("toCode"),
                            getUUID(payload, "spaceId"));
            case "RoleAssigned" ->
                    realTimeService.pushRoleAssigned(event.getTenantId(), getUUID(payload, "userId"),
                            (String) payload.get("roleCode"), getUUID(payload, "spaceId"));
            case "MemberTransferred" ->
                    realTimeService.pushMemberTransferred(event.getTenantId(), event.getAggregateId(),
                            getUUID(payload, "fromSpaceId"), getUUID(payload, "toSpaceId"));
            case "DressCodePublished" ->
                    realTimeService.pushDressCodePublished(event.getTenantId(), event.getAggregateId(),
                            getUUID(payload, "spaceId"), payload);
            case "InvitationAccepted" ->
                    realTimeService.pushInvitationAccepted(event.getTenantId(), event.getAggregateId(),
                            getUUID(payload, "userId"));
            case "TaskAssigned" ->
                    realTimeService.pushTaskAssigned(event.getTenantId(), event.getAggregateId(),
                            getUUID(payload, "assigneeId"), getUUID(payload, "spaceId"));
            case "TaskCompleted" ->
                    realTimeService.pushTaskCompleted(event.getTenantId(), event.getAggregateId(),
                            getUUID(payload, "assigneeId"), getUUID(payload, "spaceId"));
            case "PermissionsChanged" ->
                    realTimeService.pushPermissionsChanged(event.getTenantId(), getUUID(payload, "userId"),
                            (String) payload.get("changeType"));
            default -> log.debug("REALTIME consumer (no handler): {}", event.getEventType());
        }
    }

    private String buildSummary(String eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case "AssetCheckedOut" -> "Matériel sorti: " + payload.get("assetName");
            case "AssetReturned" -> "Matériel retourné: " + payload.get("assetName");
            case "AssetDamaged" -> "Matériel endommagé: " + payload.get("assetName");
            case "MemberTransferred" -> "Membre transféré: " + payload.get("memberName");
            case "RoleAssigned" -> "Rôle assigné: " + payload.get("roleName");
            case "ExpenseCreated" -> "Dépense créée: " + payload.get("amount");
            case "EventCreated" -> "Événement créé: " + payload.get("eventName");
            case "TaskCompleted" -> "Tâche terminée: " + payload.get("taskName");
            case "AttendanceRecorded" -> "Présence enregistrée";
            case "SermonPublished" -> "Prédication publiée: " + payload.get("title");
            case "DressCodePublished" -> "Tenue publiée: " + payload.get("title");
            case "SpaceConfigChanged" -> "Configuration espace modifiée";
            case "StatusChanged" -> "Statut changé: " + payload.get("fromCode") + " → " + payload.get("toCode");
            default -> eventType;
        };
    }

    private UUID getUUID(Map<String, Object> payload, String key) {
        Object v = payload.get(key);
        if (v instanceof UUID) return (UUID) v;
        if (v instanceof String) {
            try { return UUID.fromString((String) v); } catch (Exception e) { return null; }
        }
        return null;
    }
}