package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.DecisionType;
import jakarta.validation.constraints.NotNull;

/**
 * Décision d'un validateur sur une demande de transfert.
 * La motivation est obligatoire pour un refus, une demande d'informations
 * ou un renvoi pour correction.
 */
public record DecideRequest(
        @NotNull DecisionType decision,
        String motivation
) {}
