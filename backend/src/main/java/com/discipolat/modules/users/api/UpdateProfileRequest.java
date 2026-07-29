package com.discipolat.modules.users.api;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 20) String phone,
        LocalDate dateNaissance,
        String situationFamiliale
) {}
