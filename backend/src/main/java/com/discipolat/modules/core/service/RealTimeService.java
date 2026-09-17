package com.discipolat.modules.core.service;

import com.discipolat.modules.audit.domain.AuditEvent;
import com.discipolat.modules.audit.domain.BusinessHistory;
import com.discipolat.modules.core.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeService {

    private final SimpMessagingTemplate messagingTemplate;

    // ========== SPACE CONFIG CHANGES ==========

    public void pushSpaceConfigChanged(UUID tenantId, UUID spaceId, String changeType, Map<String, Object> payload) {
        String destination = "/topic/tenant:" + tenantId + "/spaces/" + spaceId + "/config";
        Map<String, Object> message = Map.of(
                "type", "SPACE_CONFIG_CHANGED",
                "changeType", changeType,
                "spaceId", spaceId.toString(),
                "payload", payload,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Pushed space config change to {}", destination);
    }

    // ========== STATUS CHANGES ==========

    public void pushStatusChanged(UUID tenantId, String entityType, UUID entityId,
                                  String fromCode, String toCode, UUID spaceId) {
        String destination = "/topic/tenant:" + tenantId + "/" + entityType.toLowerCase() + "s/" + entityId + "/status";
        Map<String, Object> message = Map.of(
                "type", "STATUS_CHANGED",
                "entityType", entityType,
                "entityId", entityId.toString(),
                "fromCode", fromCode,
                "toCode", toCode,
                "spaceId", spaceId != null ? spaceId.toString() : null,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(destination, message);

        // Also broadcast to space-level status topic
        if (spaceId != null) {
            String spaceDestination = "/topic/tenant:" + tenantId + "/spaces/" + spaceId + "/statuses";
            messagingTemplate.convertAndSend(spaceDestination, message);
        }
        log.debug("Pushed status change to {}", destination);
    }

    // ========== ROLE / PERMISSION CHANGES ==========

    public void pushRoleAssigned(UUID tenantId, UUID userId, String roleCode, UUID spaceId) {
        String userDestination = "/user/tenant:" + tenantId + ":" + userId + "/queue/roles";
        Map<String, Object> message = Map.of(
                "type", "ROLE_ASSIGNED",
                "roleCode", roleCode,
                "spaceId", spaceId != null ? spaceId.toString() : null,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + userId, "/queue/roles", message);

        if (spaceId != null) {
            String spaceDestination = "/topic/tenant:" + tenantId + "/spaces/" + spaceId + "/roles";
            messagingTemplate.convertAndSend(spaceDestination, message);
        }
        log.debug("Pushed role assigned to user {}", userId);
    }

    public void pushPermissionsChanged(UUID tenantId, UUID userId, String changeType) {
        String userDestination = "/user/tenant:" + tenantId + ":" + userId + "/queue/permissions";
        Map<String, Object> message = Map.of(
                "type", "PERMISSIONS_CHANGED",
                "changeType", changeType,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + userId, "/queue/permissions", message);
        log.debug("Pushed permissions changed to user {}", userId);
    }

    // ========== DRESS CODE ==========

    public void pushDressCodePublished(UUID tenantId, UUID dressCodeId, UUID spaceId, Map<String, Object> payload) {
        String destination = "/topic/tenant:" + tenantId + "/spaces/" + spaceId + "/dress-codes";
        Map<String, Object> message = Map.of(
                "type", "DRESS_CODE_PUBLISHED",
                "dressCodeId", dressCodeId.toString(),
                "spaceId", spaceId.toString(),
                "payload", payload,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Pushed dress code published to {}", destination);
    }

    // ========== TASK ASSIGNMENTS ==========

    public void pushTaskAssigned(UUID tenantId, UUID taskId, UUID assigneeId, UUID spaceId) {
        String userDestination = "/user/tenant:" + tenantId + ":" + assigneeId + "/queue/tasks";
        Map<String, Object> message = Map.of(
                "type", "TASK_ASSIGNED",
                "taskId", taskId.toString(),
                "spaceId", spaceId != null ? spaceId.toString() : null,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + assigneeId, "/queue/tasks", message);

        if (spaceId != null) {
            String spaceDestination = "/topic/tenant:" + tenantId + "/spaces/" + spaceId + "/tasks";
            messagingTemplate.convertAndSend(spaceDestination, message);
        }
        log.debug("Pushed task assigned to user {}", assigneeId);
    }

    public void pushTaskCompleted(UUID tenantId, UUID taskId, UUID assigneeId, UUID spaceId) {
        String userDestination = "/user/tenant:" + tenantId + ":" + assigneeId + "/queue/tasks";
        Map<String, Object> message = Map.of(
                "type", "TASK_COMPLETED",
                "taskId", taskId.toString(),
                "spaceId", spaceId != null ? spaceId.toString() : null,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + assigneeId, "/queue/tasks", message);
    }

    // ========== INVITATION ==========

    public void pushInvitationAccepted(UUID tenantId, UUID invitationId, UUID userId) {
        String userDestination = "/user/tenant:" + tenantId + ":" + userId + "/queue/invitations";
        Map<String, Object> message = Map.of(
                "type", "INVITATION_ACCEPTED",
                "invitationId", invitationId.toString(),
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + userId, "/queue/invitations", message);
    }

    // ========== MEMBER TRANSFERRED ==========

    public void pushMemberTransferred(UUID tenantId, UUID personId, UUID fromSpaceId, UUID toSpaceId) {
        // Notify the person
        String userDestination = "/user/tenant:" + tenantId + ":" + personId + "/queue/membership";
        Map<String, Object> message = Map.of(
                "type", "MEMBER_TRANSFERRED",
                "fromSpaceId", fromSpaceId != null ? fromSpaceId.toString() : null,
                "toSpaceId", toSpaceId != null ? toSpaceId.toString() : null,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + personId, "/queue/membership", message);

        // Notify both spaces
        if (fromSpaceId != null) {
            messagingTemplate.convertAndSend("/topic/tenant:" + tenantId + "/spaces/" + fromSpaceId + "/members",
                    Map.of("type", "MEMBER_REMOVED", "personId", personId.toString()));
        }
        if (toSpaceId != null) {
            messagingTemplate.convertAndSend("/topic/tenant:" + tenantId + "/spaces/" + toSpaceId + "/members",
                    Map.of("type", "MEMBER_ADDED", "personId", personId.toString()));
        }
    }

    // ========== GENERIC EVENT PUSH ==========

    public void pushEvent(UUID tenantId, String destination, String eventType, Map<String, Object> payload) {
        String fullDestination = "/topic/tenant:" + tenantId + "/" + destination;
        Map<String, Object> message = Map.of(
                "type", eventType,
                "payload", payload,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(fullDestination, message);
    }

    // ========== BROADCAST TO ALL USERS IN TENANT ==========

    public void broadcastToTenant(UUID tenantId, String eventType, Map<String, Object> payload) {
        String destination = "/topic/tenant:" + tenantId + "/broadcast";
        Map<String, Object> message = Map.of(
                "type", eventType,
                "payload", payload,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(destination, message);
    }

    // ========== NOTIFICATIONS ==========

    public void pushNotification(UUID tenantId, UUID userId, String title, String body, String type, String link) {
        String userDestination = "/user/tenant:" + tenantId + ":" + userId + "/queue/notifications";
        Map<String, Object> message = Map.of(
                "type", "NOTIFICATION",
                "title", title,
                "body", body,
                "notificationType", type,
                "link", link,
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        messagingTemplate.convertAndSendToUser("tenant:" + tenantId + ":" + userId, "/queue/notifications", message);
    }
}