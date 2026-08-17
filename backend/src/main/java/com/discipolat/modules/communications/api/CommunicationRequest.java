package com.discipolat.modules.communications.api;

import com.discipolat.modules.communications.domain.Communication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CommunicationRequest(
        @NotBlank String titre,
        @NotBlank String contenu,
        @NotNull Communication.Cible cible,
        List<String> roles,
        UUID familleId,
        UUID departmentId
) {
}
