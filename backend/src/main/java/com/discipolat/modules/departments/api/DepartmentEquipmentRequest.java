package com.discipolat.modules.departments.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Création / mise à jour d'un équipement (inventaire matériel) du département.
 *
 * @param nom             nom de l'équipement
 * @param description     description (optionnel)
 * @param quantite        quantité (défaut 1)
 * @param etat            NEUF | BON | USAGE | REPARATION | HORS_SERVICE
 * @param responsableId   membre responsable (optionnel)
 * @param affecteAId      membre à qui l'équipement est affecté (optionnel)
 * @param localisation    localisation (optionnel)
 * @param dateAcquisition date d'acquisition (optionnel)
 */
public record DepartmentEquipmentRequest(
        @NotBlank String nom,
        String description,
        @Min(1) Integer quantite,
        String etat,
        UUID responsableId,
        UUID affecteAId,
        String localisation,
        LocalDate dateAcquisition
) {}
