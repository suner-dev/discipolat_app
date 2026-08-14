package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentDocument;
import jakarta.validation.constraints.NotBlank;

/**
 * Création / mise à jour d'un document du département.
 * {@code type} : PROCEDURE | GUIDE | DOCUMENT | FORMULAIRE | COMPTE_RENDU | RESSOURCE.
 */
public record DepartmentDocumentRequest(
        @NotBlank String titre,
        DepartmentDocument.DocumentType type,
        String description,
        String url,
        String statut
) {
}
