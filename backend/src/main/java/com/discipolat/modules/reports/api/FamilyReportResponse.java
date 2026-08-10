package com.discipolat.modules.reports.api;

import com.discipolat.common.enums.StatutValidation;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.reports.domain.FamilyReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FamilyReportResponse(
        UUID id,
        UUID familleId,
        UUID chefFamilleId,
        LocalDate semaine,
        Map<String, Object> statsAgregees,
        BigDecimal presenceMoyenne,
        Integer totalPresents,
        Integer totalAbsents,
        Integer totalSorties,
        Map<String, Object> repartitionSorties,
        Integer totalMaintenus,
        Integer nbSuivisParalleles,
        Map<String, Object> suivisParallelesDetails,
        Map<String, Object> faiseursSansRapport,
        String commentaireSynthese,
        StatutValidation statutValidation,
        LocalDateTime dateSoumission,
        LocalDateTime createdAt,
        List<EntityAttachmentService.AttachmentItem> piecesJointes
) {
    public static FamilyReportResponse from(FamilyReport report, List<EntityAttachmentService.AttachmentItem> piecesJointes) {
        return new FamilyReportResponse(
                report.getId(), report.getFamilleId(), report.getChefFamilleId(),
                report.getSemaine(), report.getStatsAgregees(),
                report.getPresenceMoyenne(), report.getTotalPresents(),
                report.getTotalAbsents(), report.getTotalSorties(),
                report.getRepartitionSorties(), report.getTotalMaintenus(),
                report.getNbSuivisParalleles(), report.getSuivisParallelesDetails(),
                report.getFaiseursSansRapport(), report.getCommentaireSynthese(),
                report.getStatutValidation(), report.getDateSoumission(), report.getCreatedAt(),
                piecesJointes != null ? piecesJointes : List.of());
    }
}
