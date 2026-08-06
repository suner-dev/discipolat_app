package com.discipolat.modules.members.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Saisie groupée des présences d'un département pour une semaine :
 * le responsable coche présent/absent pour chaque membre.
 */
public record SubmitDepartmentPresenceRequest(
        @NotNull LocalDate semaine,
        String typeProgramme,
        String sousProgramme,
        @NotNull @Valid List<SubmitDepartmentPresenceItem> presences
) {}
