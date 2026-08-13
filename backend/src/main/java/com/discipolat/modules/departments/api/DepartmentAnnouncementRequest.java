package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentAnnouncement;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** Création d'une annonce du département (communication interne). */
public record DepartmentAnnouncementRequest(
        @NotBlank String titre,
        @NotBlank String message,
        DepartmentAnnouncement.Cible cible,
        UUID teamId,
        UUID positionId
) {}
