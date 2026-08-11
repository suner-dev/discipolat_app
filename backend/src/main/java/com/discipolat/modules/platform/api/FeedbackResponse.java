package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.Feedback;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vue admin d'un retour testeur : contenu + statut + émetteur (email résolu
 * depuis le compte utilisateur — aucune donnée personnelle supplémentaire).
 */
public record FeedbackResponse(
        UUID id,
        String category,
        String priority,
        String subject,
        String description,
        String pageUrl,
        String browser,
        String device,
        String os,
        String appVersion,
        String status,
        UUID createdBy,
        String reporterEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FeedbackResponse from(Feedback f, String reporterEmail) {
        return new FeedbackResponse(
                f.getId(), f.getCategory(), f.getPriority(), f.getSubject(),
                f.getDescription(), f.getPageUrl(), f.getBrowser(), f.getDevice(),
                f.getOs(), f.getAppVersion(), f.getStatus(), f.getCreatedBy(),
                reporterEmail, f.getCreatedAt(), f.getUpdatedAt());
    }
}
