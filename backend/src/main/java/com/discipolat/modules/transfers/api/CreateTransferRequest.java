package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.PrioriteTransfert;
import com.discipolat.common.enums.TransferType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Création d'une demande de transfert.
 * L'affectation actuelle est calculée côté serveur si elle n'est pas fournie.
 */
public record CreateTransferRequest(
        @NotNull TransferType type,
        @NotNull UUID personneId,
        String personneType,
        Map<String, Object> ancienneAffectation,
        @NotNull Map<String, Object> nouvelleAffectation,
        @NotBlank String justification,
        PrioriteTransfert priorite,
        String commentaires,
        List<UUID> fichierIds,
        /** Règles d'exécution spécifiques à cette demande (ex : transfererAmes). */
        Map<String, Object> reglesExecution
) {}
