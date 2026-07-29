package com.discipolat.modules.families.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReassignChiefRequest(
        @NotNull UUID newChefId
) {}
