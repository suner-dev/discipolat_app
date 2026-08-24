package com.discipolat.modules.messages.api;

import jakarta.validation.constraints.NotBlank;

public record ToggleReactionRequest(
        @NotBlank String emoji
) {}
