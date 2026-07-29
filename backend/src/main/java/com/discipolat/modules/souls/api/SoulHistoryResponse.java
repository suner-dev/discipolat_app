package com.discipolat.modules.souls.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record SoulHistoryResponse(
        UUID id,
        UUID ameId,
        String typeEvenement,
        String description,
        String ancienStatut,
        String nouveauStatut,
        UUID utilisateurId,
        LocalDateTime createdAt
) {}
