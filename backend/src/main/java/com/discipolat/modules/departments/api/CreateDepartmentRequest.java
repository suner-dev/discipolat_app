package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDepartmentRequest(
        @NotBlank String nom,
        String description,
        @NotNull UUID responsableId
) {}
