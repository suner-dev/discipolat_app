package com.discipolat.modules.users.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferFaiseurRequest(
    @NotNull(message = "La nouvelle famille est requise")
    UUID nouvelleFamilleId,

    boolean transfererAmes
) {}
