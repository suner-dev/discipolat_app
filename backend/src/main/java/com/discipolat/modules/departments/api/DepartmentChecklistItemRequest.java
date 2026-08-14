package com.discipolat.modules.departments.api;

/**
 * Ajout / mise à jour d'un élément de checklist.
 *
 * @param libelle libellé de l'élément (requis à l'ajout, optionnel au toggle)
 * @param fait    état coché de l'élément
 */
public record DepartmentChecklistItemRequest(
        String libelle,
        Boolean fait
) {}
