package com.discipolat.modules.families.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Création d'une famille de disciples.
 *
 * Cas 1 : Sélectionner un chef existant (chefFamilleId requis)
 * Cas 2 : Créer immédiatement un nouveau chef (createNewChef = true + infos du chef)
 */
public record CreateFamilyRequest(
        @NotBlank String nom,
        UUID chefFamilleId,
        UUID chefAdjointId,

        // Cas 2 : Créer un nouveau chef
        Boolean createNewChef,
        String newChefFirstName,
        String newChefLastName,
        @Email String newChefEmail,
        String newChefPhone,
        String newChefSexe,
        String newChefDateNaissance,
        String newChefAdresse
) {
    /**
     * Vérifie si on doit créer un nouveau chef de famille.
     */
    public boolean shouldCreateNewChef() {
        return Boolean.TRUE.equals(createNewChef)
                && newChefFirstName != null && !newChefFirstName.isBlank()
                && newChefLastName != null && !newChefLastName.isBlank()
                && newChefEmail != null && !newChefEmail.isBlank();
    }
}
