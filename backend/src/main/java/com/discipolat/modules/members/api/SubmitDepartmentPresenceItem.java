package com.discipolat.modules.members.api;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Saisie de présence individuelle d'un membre (effectuée par le responsable).
 */
public record SubmitDepartmentPresenceItem(
        @NotNull UUID soulId,
        @NotNull Boolean present,
        Map<String, Boolean> presences,
        String notes
) {}
