package com.discipolat.modules.transfers.api;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Entrée de l'historique immuable d'une demande de transfert. */
public record TransferHistoryResponse(
        UUID id,
        String action,
        String ancienStatut,
        String nouveauStatut,
        UUID utilisateurId,
        String utilisateurNom,
        String roleActif,
        String commentaire,
        Map<String, Object> ancienneValeur,
        Map<String, Object> nouvelleValeur,
        LocalDateTime createdAt
) {}
