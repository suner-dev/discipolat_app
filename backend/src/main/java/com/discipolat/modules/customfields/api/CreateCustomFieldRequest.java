package com.discipolat.modules.customfields.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Requête de création d'une définition de champ personnalisé.
 * Remplace l'utilisation directe de l'entité JPA comme @RequestBody.
 */
public record CreateCustomFieldRequest(
    @NotBlank(message = "L'entité type est requis")
    String entiteType,

    @NotBlank(message = "Le code est requis")
    String code,

    @NotBlank(message = "Le label est requis")
    String label,

    @NotBlank(message = "Le type est requis")
    String type,

    boolean obligatoire,

    int ordre,

    List<String> options,

    String placeholder,

    String defaultValue,

    List<String> rolesLecture,

    List<String> rolesEcriture
) {}
