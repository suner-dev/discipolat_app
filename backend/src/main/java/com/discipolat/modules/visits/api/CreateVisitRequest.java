package com.discipolat.modules.visits.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateVisitRequest(
        @NotNull java.util.UUID soulId,
        @NotNull LocalDate datePrevue,
        String motif,
        String objectif
) {
}
