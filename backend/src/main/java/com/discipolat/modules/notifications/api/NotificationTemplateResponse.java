package com.discipolat.modules.notifications.api;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.notifications.domain.NotificationTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Vue d'un modèle de notification (administration). */
public record NotificationTemplateResponse(
        UUID id,
        TypeNotification event,
        String titre,
        String message,
        List<CanalNotification> canaux,
        List<String> rolesDestinataires,
        boolean actif,
        LocalDateTime updatedAt
) {
    public static NotificationTemplateResponse from(NotificationTemplate t) {
        return new NotificationTemplateResponse(
                t.getId(), t.getEvent(), t.getTitre(), t.getMessage(),
                t.getCanaux() != null ? t.getCanaux() : List.of(),
                t.getRolesDestinataires() != null ? t.getRolesDestinataires() : List.of(),
                t.isActif(), t.getUpdatedAt());
    }
}
