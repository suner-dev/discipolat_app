package com.discipolat.modules.events.api;

import com.discipolat.modules.events.domain.EventRegistration;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventRegistrationResponse(
        UUID id,
        UUID eventId,
        UUID utilisateurId,
        String statutInscription,
        LocalDateTime dateInscription,
        LocalDateTime dateEmargement
) {
    public static EventRegistrationResponse from(EventRegistration reg) {
        return new EventRegistrationResponse(
                reg.getId(), reg.getEventId(), reg.getUtilisateurId(),
                reg.getStatutInscription(), reg.getDateInscription(), reg.getDateEmargement());
    }
}
