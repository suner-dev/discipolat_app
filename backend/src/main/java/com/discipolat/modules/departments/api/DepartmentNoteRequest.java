package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotBlank;

/** Création d'une note départementale sur un membre. */
public record DepartmentNoteRequest(
        @NotBlank String contenu
) {}
