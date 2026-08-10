package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.PrioriteTransfert;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mise à jour d'une demande en brouillon (justification, cible, priorités, pièces jointes). */
public record UpdateTransferRequest(
        Map<String, Object> nouvelleAffectation,
        String justification,
        PrioriteTransfert priorite,
        String commentaires,
        List<UUID> fichierIds
) {}
