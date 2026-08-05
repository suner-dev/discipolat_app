package com.discipolat.modules.appointments.api;

import com.discipolat.modules.appointments.domain.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID demandeurId,
        String demandeurNom,
        UUID recepteurId,
        String recepteurNom,
        Appointment.Motif motif,
        String objet,
        LocalDateTime datePrevue,
        int dureeMinutes,
        Appointment.Statut statut,
        String reponse,
        LocalDateTime dateTraitement,
        boolean rappelEnvoye,
        LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment a, String demandeurNom, String recepteurNom) {
        return new AppointmentResponse(
                a.getId(), a.getDemandeurId(), demandeurNom, a.getRecepteurId(), recepteurNom,
                a.getMotif(), a.getObjet(), a.getDatePrevue(), a.getDureeMinutes(), a.getStatut(),
                a.getReponse(), a.getDateTraitement(), a.isRappelEnvoye(), a.getCreatedAt());
    }
}
