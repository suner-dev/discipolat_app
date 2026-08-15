package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Pointage d'un membre (âme) du département à un événement. */
public record DepartmentEventAttendanceRequest(
        @NotNull UUID soulId,
        @NotNull Boolean present
) {}
