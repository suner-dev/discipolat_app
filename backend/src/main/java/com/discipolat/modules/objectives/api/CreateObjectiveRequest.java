package com.discipolat.modules.objectives.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.objectives.domain.Objective;
import com.discipolat.modules.objectives.domain.ObjectiveType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateObjectiveRequest(
        @NotNull UserRole role,
        @NotNull ObjectiveType type,
        @Min(1) int cible,
        @NotNull Objective.Periode periode
) {
}
