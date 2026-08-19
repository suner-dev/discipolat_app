package com.discipolat.modules.users.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record ReplaceRolesRequest(
    @NotEmpty(message = "La liste des rôles est requise")
    Set<String> roles
) {}
