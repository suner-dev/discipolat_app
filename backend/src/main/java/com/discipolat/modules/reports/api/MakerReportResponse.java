package com.discipolat.modules.reports.api;

import com.discipolat.common.enums.MotifSortie;
import com.discipolat.common.enums.RaisonAbsence;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.reports.domain.MakerReport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MakerReportResponse(
        UUID id,
        UUID faiseurId,
        UUID ameId,
        LocalDate semaine,
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
        boolean soumis,
        LocalDateTime dateSoumission,
        LocalDateTime createdAt,
        List<EntityAttachmentService.AttachmentItem> piecesJointes
) {
    public static MakerReportResponse from(MakerReport report, List<EntityAttachmentService.AttachmentItem> piecesJointes) {
        return new MakerReportResponse(
                report.getId(), report.getFaiseurId(), report.getAmeId(),
                report.getSemaine(),                report.getPresencesParCulte(),
                report.getAbsencesMulti(),
                report.getAbsenceRaison(), report.getAbsenceCommentaire(),
                report.getDifficultesCategorie(), report.getDifficultes(),
                report.getNbSorties(), report.getMotifSortie(),
                report.getNbMaintenus(), report.getNbInvitesCulte(),
                report.getVieFaiseurChallenges(), report.getVieFaiseurDemandesAide(),
                report.getVieFaiseurSuggestions(), report.getNotesComplementaires(),
                report.isSoumis(), report.getDateSoumission(), report.getCreatedAt(),
                piecesJointes != null ? piecesJointes : List.of());
    }
}
