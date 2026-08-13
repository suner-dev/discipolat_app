package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentTask;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Création / modification d'une tâche du département.
 */
public record DepartmentTaskRequest(
        @NotBlank String titre,
        String description,
        UUID teamId,
        UUID assignedTo,
        DepartmentTask.TaskPriority priorite,
        DepartmentTask.TaskStatus statut,
        LocalDate dateDebut,
        LocalDate echeance,
        Integer avancement
) {
}
