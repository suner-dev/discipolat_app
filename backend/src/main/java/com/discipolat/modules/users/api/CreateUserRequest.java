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
        UserRole activeRole
) {}
