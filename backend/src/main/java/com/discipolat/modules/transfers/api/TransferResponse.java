package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.PrioriteTransfert;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TransferType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Vue d'une demande de transfert avec noms résolus pour l'affichage. */
public record TransferResponse(
        UUID id,
        TransferType type,
        TransferStatus statut,
        UUID personneId,
        String personneType,
        String personneNom,
        Map<String, Object> ancienneAffectation,
        Map<String, Object> nouvelleAffectation,
        UUID demandeurId,
        String demandeurNom,
        String justification,
        PrioriteTransfert priorite,
        String commentaires,
        LocalDateTime dateSoumission,
        LocalDateTime dateExecution,
        LocalDateTime delaiLimite,
        Integer etapeCourante,
        Integer approbationsObtenues,
        Integer totalEtapes,
        LocalDateTime createdAt
) {}
