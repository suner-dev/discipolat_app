package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/** Lignes CSV d'import de membres (clés : nom, prenom, email, telephone, profession, ville, equipe, poste). */
public record DepartmentImportRequest(
        @NotNull List<Map<String, Object>> rows
) {}
