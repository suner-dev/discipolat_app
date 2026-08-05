package com.discipolat.modules.appointments.api;

import com.discipolat.modules.appointments.domain.Appointment;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
        @NotNull Appointment.Statut statut,
        String reponse
) {
}
