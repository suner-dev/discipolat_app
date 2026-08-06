package com.discipolat.modules.programs.api;

import com.discipolat.modules.programs.domain.ProgramSubType;
import com.discipolat.modules.programs.domain.ProgramType;

import java.util.List;
import java.util.UUID;

public record ProgramTypeResponse(
        UUID id,
        String code,
        String label,
        String description,
        boolean aSousProgrammes,
        String couleur,
        boolean actif,
        Integer ordre,
        List<ProgramSubTypeResponse> sousProgrammes
) {
    public static ProgramTypeResponse from(ProgramType type, List<ProgramSubType> subTypes) {
        List<ProgramSubTypeResponse> subs = subTypes == null ? List.of()
                : subTypes.stream().map(ProgramSubTypeResponse::from).toList();
        return new ProgramTypeResponse(
                type.getId(), type.getCode(), type.getLabel(), type.getDescription(),
                type.isASousProgrammes(), type.getCouleur(), type.isActif(), type.getOrdre(), subs);
    }

    public record ProgramSubTypeResponse(
            UUID id,
            String label,
            String heureDebut,
            String heureFin,
            boolean actif,
            Integer ordre
    ) {
        public static ProgramSubTypeResponse from(ProgramSubType sub) {
            return new ProgramSubTypeResponse(
                    sub.getId(), sub.getLabel(),
                    sub.getHeureDebut() != null ? sub.getHeureDebut().toString() : null,
                    sub.getHeureFin() != null ? sub.getHeureFin().toString() : null,
                    sub.isActif(), sub.getOrdre());
        }
    }
}
