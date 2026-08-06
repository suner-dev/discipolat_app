package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Création d'un département.
 *
 * Cas 1 : Sélectionner un responsable existant (responsableId requis)
 * Cas 2 : Créer immédiatement un nouveau responsable (createNewResponsable = true + infos)
 */
public record CreateDepartmentRequest(
        @NotBlank String nom,
        String description,
        UUID responsableId,

        // Cas 2 : Créer un nouveau responsable
        Boolean createNewResponsable,
        String newRespFirstName,
        String newRespLastName,
        @Email String newRespEmail,
        String newRespPhone
) {
    public boolean shouldCreateNewResponsable() {
        return Boolean.TRUE.equals(createNewResponsable)
                && newRespFirstName != null && !newRespFirstName.isBlank()
                && newRespLastName != null && !newRespLastName.isBlank()
                && newRespEmail != null && !newRespEmail.isBlank();
    }
}
