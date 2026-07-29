package com.discipolat.modules.parallelfollowups.api;

import com.discipolat.common.enums.RaisonSuiviParallele;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateParallelFollowupRequest(
        @NotNull UUID ameId,
        @NotNull UUID initiateurId,
        UUID familleId,
        @NotNull RaisonSuiviParallele raison,
        String raisonDetail,
        @NotNull LocalDate dateDebut
) {}
