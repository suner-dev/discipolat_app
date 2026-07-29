package com.discipolat.modules.departments.api;

import com.discipolat.common.enums.StatutEntite;
import com.discipolat.modules.departments.domain.Department;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String nom,
        String description,
        UUID responsableId,
        StatutEntite statut,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DepartmentResponse from(Department dept) {
        return new DepartmentResponse(
                dept.getId(), dept.getNom(), dept.getDescription(),
                dept.getResponsableId(), dept.getStatut(),
                dept.getCreatedAt(), dept.getUpdatedAt());
    }
}
