package com.discipolat.modules.programs.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ProgramTypeRequest(
        @NotBlank String code,
        @NotBlank String label,
        String description,
        boolean aSousProgrammes,
        String couleur,
        boolean actif,
        Integer ordre,
        List<SubTypeRequest> sousProgrammes
) {
    public record SubTypeRequest(
            String id,
            @NotBlank String label,
            String heureDebut,
            String heureFin,
            boolean actif,
            Integer ordre
    ) {}
}
