package com.discipolat.modules.authentication.api;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
    @NotBlank(message = "Le token est requis")
    String token
) {}
