package com.discipolat.modules.souls.api;

import jakarta.validation.constraints.NotBlank;

public record CreateSoulNoteRequest(
        @NotBlank String contenu
) {}
