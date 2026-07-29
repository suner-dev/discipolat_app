package com.discipolat.modules.parallelfollowups.api;

import com.discipolat.common.enums.RaisonSuiviParallele;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowup;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ParallelFollowupResponse(
        UUID id,
        UUID ameId,
        UUID initiateurId,
        UUID familleId,
        RaisonSuiviParallele raison,
        String raisonDetail,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutSuiviParallele statut,
        LocalDateTime createdAt
) {
    public static ParallelFollowupResponse from(ParallelFollowup f) {
        return new ParallelFollowupResponse(
                f.getId(), f.getAmeId(), f.getInitiateurId(), f.getFamilleId(),
                f.getRaison(), f.getRaisonDetail(), f.getDateDebut(),
                f.getDateFin(), f.getStatut(), f.getCreatedAt());
    }
}
