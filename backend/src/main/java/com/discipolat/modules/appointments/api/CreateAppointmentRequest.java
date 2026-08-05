package com.discipolat.modules.appointments.api;

import com.discipolat.modules.appointments.domain.Appointment;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID recepteurId,
        @NotNull Appointment.Motif motif,
        String objet,
        @NotNull LocalDateTime datePrevue,
        Integer dureeMinutes
) {
}
