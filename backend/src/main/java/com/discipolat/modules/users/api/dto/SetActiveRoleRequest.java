package com.discipolat.modules.users.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SetActiveRoleRequest(
    @NotBlank(message = "Le rôle actif est requis")
    String activeRole
) {}
