package com.discipolat.modules.reports.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SubmitFamilyReportRequest(
        @NotNull UUID familleId,
        @NotNull UUID chefFamilleId,
        @NotNull LocalDate semaine,
        Map<String, Object> statsAgregees,
        Integer totalSorties,
        Integer totalMaintenus,
        Integer nbSuivisParalleles,
        Map<String, Object> suivisParallelesDetails,
        Map<String, Object> faiseursSansRapport,
        String commentaireSynthese,
        List<UUID> fichierIds
) {}
