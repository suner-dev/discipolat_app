package com.discipolat.modules.authentication.api;

import jakarta.validation.constraints.NotBlank;

public record SwitchRoleRequest(
    @NotBlank(message = "Le rôle est requis")
    String role
) {}
