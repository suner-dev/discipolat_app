package com.discipolat.modules.souls.api;

import com.discipolat.modules.souls.domain.SoulNote;

import java.time.LocalDateTime;
import java.util.UUID;

public record SoulNoteResponse(
        UUID id,
        UUID ameId,
        UUID auteurId,
        String contenu,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SoulNoteResponse from(SoulNote note) {
        return new SoulNoteResponse(
                note.getId(), note.getAmeId(), note.getAuteurId(),
                note.getContenu(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
