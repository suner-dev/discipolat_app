package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Ajout d'une personne déjà inscrite sur la plateforme à un département.
 */
public record DepartmentMemberRequest(
        @NotNull UUID soulId
) {}
