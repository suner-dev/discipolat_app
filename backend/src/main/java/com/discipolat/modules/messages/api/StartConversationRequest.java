package com.discipolat.modules.messages.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartConversationRequest(
        @NotNull UUID otherUserId
) {
}
