package com.discipolat.modules.visits.api;

import com.discipolat.modules.visits.domain.Visit;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateVisitRequest(
        @NotNull Visit.StatutVisite statut,
        LocalDate dateRealisee,
        String compteRendu,
        String photoUrl,
        Boolean present,
        LocalDate datePrevue
) {
}
