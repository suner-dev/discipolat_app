package com.discipolat.modules.platform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps d'une soumission de retour testeur. Les informations techniques
 * (navigateur, OS, appareil, page) sont capturées côté client et envoyées
 * telles quelles ; seules les données utiles au traitement sont stockées.
 */
public record CreateFeedbackRequest(
        @NotBlank @Size(max = 50) String category,
        @Size(max = 20) String priority,
        @NotBlank @Size(max = 255) String subject,
        @Size(max = 5000) String description,
        @Size(max = 500) String pageUrl,
        @Size(max = 500) String userAgent,
        @Size(max = 200) String browser,
        @Size(max = 200) String device,
        @Size(max = 200) String os
) {}
