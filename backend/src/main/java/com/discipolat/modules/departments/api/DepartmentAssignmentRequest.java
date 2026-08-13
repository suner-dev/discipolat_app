package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentAssignment;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Affectation d'un membre (soul_id) à une équipe et/ou un poste,
 * avec période et rôle.
 */
public record DepartmentAssignmentRequest(
        @NotNull UUID memberId,
        UUID teamId,
        UUID positionId,
        DepartmentAssignment.AssignmentRole role,
        LocalDate dateDebut,
        LocalDate dateFin
) {
}
