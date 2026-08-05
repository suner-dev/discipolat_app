package com.discipolat.modules.appointments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByDemandeurIdOrderByDatePrevueDesc(UUID demandeurId);
    List<Appointment> findByRecepteurIdOrderByDatePrevueDesc(UUID recepteurId);

    /** RDV confirmés à venir entre deux instants (rappel). */
    List<Appointment> findByStatutAndDatePrevueBetweenAndRappelEnvoyeFalse(
            Appointment.Statut statut, LocalDateTime from, LocalDateTime to);
}
