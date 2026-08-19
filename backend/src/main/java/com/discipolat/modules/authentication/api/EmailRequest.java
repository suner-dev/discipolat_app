package com.discipolat.modules.authentication.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    String email
) {}
