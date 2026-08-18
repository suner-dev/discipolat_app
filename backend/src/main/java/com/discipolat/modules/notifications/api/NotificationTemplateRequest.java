package com.discipolat.modules.notifications.api;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;

import java.util.List;

/** Paramétrage d'un modèle de notification par l'administrateur. */
public record NotificationTemplateRequest(
        TypeNotification event,
        String titre,
        String message,
        List<CanalNotification> canaux,
        List<String> rolesDestinataires,
        Boolean actif
) {}
