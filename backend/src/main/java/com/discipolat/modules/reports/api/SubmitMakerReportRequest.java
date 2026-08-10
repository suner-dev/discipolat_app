package com.discipolat.modules.reports.api;

import com.discipolat.common.enums.MotifSortie;
import com.discipolat.common.enums.RaisonAbsence;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SubmitMakerReportRequest(
        @NotNull UUID faiseurId,
        @NotNull UUID ameId,
        @NotNull LocalDate semaine,
        Map<String, Boolean> presencesParCulte,
        List<String> absencesMulti,
        RaisonAbsence absenceRaison,
        String absenceCommentaire,
        String difficultesCategorie,
        String difficultes,
        Integer nbSorties,
        MotifSortie motifSortie,
        Integer nbMaintenus,
        Integer nbInvitesCulte,
        String vieFaiseurChallenges,
        String vieFaiseurDemandesAide,
        String vieFaiseurSuggestions,
        String notesComplementaires,
        List<UUID> fichierIds
) {}
