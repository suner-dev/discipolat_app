package com.discipolat.modules.members.api;

import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.members.domain.MemberRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MemberRequestResponse(
        UUID id,
        MemberRequest.Type type,
        MemberRequest.Cible cible,
        String message,
        MemberRequest.Statut statut,
        String reponse,
        UUID traitePar,
        String traiteParNom,
        LocalDateTime dateTraitement,
        LocalDateTime createdAt,
        UUID auteurId,
        String auteurNom,
        UUID departmentId,
        String departmentNom,
        UUID familyId,
        String familyNom,
        List<EntityAttachmentService.AttachmentItem> piecesJointes
) {
    public static MemberRequestResponse from(MemberRequest r, String auteurNom,
                                             String traiteParNom, String departmentNom, String familyNom,
                                             List<EntityAttachmentService.AttachmentItem> piecesJointes) {
        return new MemberRequestResponse(
                r.getId(), r.getType(), r.getCible(), r.getMessage(), r.getStatut(),
                r.getReponse(), r.getTraitePar(), traiteParNom, r.getDateTraitement(),
                r.getCreatedAt(), r.getUserId(), auteurNom,
                r.getDepartmentId(), departmentNom, r.getFamilyId(), familyNom,
                piecesJointes != null ? piecesJointes : List.of());
    }
}
