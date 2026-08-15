package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Présence d'un membre (âme) du département à un événement rattaché au
 * département. Permet au responsable — mais aussi au chef de famille ou au
 * faiseur de l'âme — de pointer un membre présent/absent à un événement.
 */
@Entity
@Table(name = "department_event_attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentEventAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "soul_id", nullable = false)
    private UUID soulId;

    @Column(name = "present", nullable = false)
    private boolean present;

    /** Utilisateur qui a marqué la présence (responsable, chef, faiseur…). */
    @Column(name = "marked_by", nullable = false)
    private UUID markedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
