package com.discipolat.modules.souls.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReassignSoulRequest(
        @NotNull UUID newFaiseurId
) {}
