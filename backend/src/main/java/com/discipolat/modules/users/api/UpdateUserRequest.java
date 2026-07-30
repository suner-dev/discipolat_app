package com.discipolat.modules.users.api;

import com.discipolat.common.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UpdateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        UserRole role,
        Set<UserRole> roles,
        UserRole activeRole
) {}
