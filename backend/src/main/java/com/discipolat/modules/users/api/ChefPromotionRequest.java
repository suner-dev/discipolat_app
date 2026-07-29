package com.discipolat.modules.users.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChefPromotionRequest(
        @NotNull UUID familleId
) {}
