package com.discipolat.modules.members.api;

import com.discipolat.modules.members.domain.MemberPresence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record MemberPresenceResponse(
        UUID id,
        UUID userId,
        String nomMembre,
        LocalDate semaine,
        Map<String, Boolean> presences,
        String notes,
        String typeProgramme,
        String sousProgramme,
        Boolean present,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MemberPresenceResponse from(MemberPresence p, String nomMembre) {
        return new MemberPresenceResponse(
                p.getId(), p.getUserId(), nomMembre, p.getSemaine(),
                p.getPresences(), p.getNotes(), p.getTypeProgramme(), p.getSousProgramme(),
                p.getPresent(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
