package com.discipolat.modules.authentication.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UUID userId,
        String email,
        String role,
        List<String> roles,
        String activeRole,
        boolean estChefDeFamille,
        String firstName,
        String lastName,
        Boolean twoFactorEnabled
) {}
