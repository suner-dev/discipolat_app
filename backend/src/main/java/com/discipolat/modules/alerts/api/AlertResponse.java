package com.discipolat.modules.alerts.api;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.modules.alerts.domain.Alert;
import java.time.LocalDateTime;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID ameId,
        UUID faiseurId,
        UUID familleId,
        String typeAlerte,
        String message,
        LocalDateTime dateDeclenchement,
        StatutAlerte statut,
        LocalDateTime dateResolution,
        UUID resoluPar
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(), alert.getAmeId(), alert.getFaiseurId(),
                alert.getFamilleId(), alert.getTypeAlerte(), alert.getMessage(),
                alert.getDateDeclenchement(), alert.getStatut(),
                alert.getDateResolution(), alert.getResoluPar());
    }
}
