package com.discipolat.modules.notifications.api;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;

import java.util.List;

/**
 * Métadonnées d'un événement de notification (catalogue admin) : libellé
 * français, modèle suggéré par défaut et variables disponibles pour le rendu.
 */
public record NotificationEventInfo(
        TypeNotification event,
        String label,
        String defaultTitre,
        String defaultMessage,
        List<CanalNotification> canauxSuggestes,
        List<String> variables
) {}
