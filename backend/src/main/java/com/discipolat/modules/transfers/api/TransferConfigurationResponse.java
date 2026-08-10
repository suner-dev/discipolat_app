package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.TransferType;

import java.util.List;
import java.util.UUID;

/** Configuration d'un type de transfert telle que vue par un initiateur. */
public record TransferConfigurationResponse(
        UUID id,
        TransferType type,
        String label,
        String description,
        boolean actif,
        List<String> rolesInitiateurs,
        boolean canInitier,
        List<String> etapes
) {}
