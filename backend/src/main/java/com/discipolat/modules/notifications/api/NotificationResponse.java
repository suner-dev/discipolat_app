package com.discipolat.modules.notifications.api;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.notifications.domain.Notification;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID destinataireId,
        TypeNotification type,
        CanalNotification canal,
        String titre,
        String message,
        boolean lu,
        LocalDateTime dateLecture,
        UUID entiteReferenceId,
        String entiteReferenceType,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getDestinataireId(), n.getType(), n.getCanal(),
                n.getTitre(), n.getMessage(), n.isLu(), n.getDateLecture(),
                n.getEntiteReferenceId(), n.getEntiteReferenceType(), n.getCreatedAt());
    }
}
