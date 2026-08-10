package com.discipolat.modules.transfers.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Détail complet d'une demande de transfert : étapes du circuit, décisions, pièces jointes. */
public record TransferDetailResponse(
        TransferResponse transfert,
        List<EtapeValidation> etapes,
        List<DecisionItem> decisions,
        List<AttachmentItem> piecesJointes,
        boolean peutValider,
        String roleActif,
        String modeValidation
) {
    public record EtapeValidation(
            UUID id,
            Integer etapeOrdre,
            List<String> rolesValidateurs,
            String label,
            String description,
            boolean requis,
            boolean validee
    ) {}

    public record DecisionItem(
            UUID id,
            UUID validateurId,
            String validateurNom,
            String roleValidateur,
            String decision,
            String motivation,
            Integer etapeOrdre,
            LocalDateTime createdAt
    ) {}

    public record AttachmentItem(
            UUID id,
            UUID fileId,
            String nom,
            String url,
            LocalDateTime createdAt
    ) {}
}
