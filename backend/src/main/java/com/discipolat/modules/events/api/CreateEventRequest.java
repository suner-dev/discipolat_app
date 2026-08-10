package com.discipolat.modules.events.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateEventRequest(
        @NotBlank String typeEvenement,
        @NotBlank String titre,
        String description,
        String lieu,
        @NotNull LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer limitePlaces,
        UUID familleId,
        List<UUID> fichierIds
) {}
