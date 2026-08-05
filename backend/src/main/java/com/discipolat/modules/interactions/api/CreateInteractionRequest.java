package com.discipolat.modules.interactions.api;

import com.discipolat.modules.interactions.domain.InteractionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateInteractionRequest(
        @NotNull InteractionType type,
        String canal,
        @Size(max = 200) String objet,
        @Size(max = 4000) String contenu,
        LocalDateTime dateInteraction,
        UUID aFairePar,
        LocalDateTime rappelLe
) {
}
