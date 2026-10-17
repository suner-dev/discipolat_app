package com.discipolat.modules.users.api;

import com.discipolat.common.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @NotBlank String password,
        @NotNull UserRole role,
        Set<UserRole> roles,
        UserRole activeRole,
        // P7b — contrat élargi : clé d'un rôle custom du catalogue SERVI du
        // tenant, assignable à la création (au-delà de l'enum socle UserRole).
        // Optionnelle ; si présente, la tenant_membership ACTIVE de portée
        // TENANT du compte est pointée sur ce rôle du catalogue (pont V173)
        // et les permissions servies sont rafraîchies. Clé inconnue → 4xx.
        String customRole
) {}
