package com.discipolat.modules.members.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMemberProfileRequest(
        @Size(max = 20) String phone,
        @Size(max = 255) String photoUrl,
        String situationFamiliale,
        LocalDate dateNaissance,
        @Size(max = 255) String profession,
        @Size(max = 150) String niveauEtude,
        @Min(0) @Max(50) Integer nbEnfants
) {}
