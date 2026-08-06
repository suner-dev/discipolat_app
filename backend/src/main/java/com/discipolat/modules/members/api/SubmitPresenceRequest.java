package com.discipolat.modules.members.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

public record SubmitPresenceRequest(
        @NotNull LocalDate semaine,
        @NotNull Map<String, Boolean> presences,
        @Size(max = 2000) String notes,
        String typeProgramme,
        String sousProgramme
) {}
