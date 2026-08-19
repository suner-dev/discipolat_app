package com.discipolat.modules.users.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddRoleRequest(
    @NotBlank(message = "Le rôle est requis")
    String role
) {}
