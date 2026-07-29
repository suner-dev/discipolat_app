package com.discipolat.modules.souls.api;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateSoulRetractionRequest(
        @NotBlank UUID ameId,
        @NotBlank String justification
) {}
