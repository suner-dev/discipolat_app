package com.discipolat.modules.tickets.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête de création d'un ticket de support.
 * `categorie` et `priorite` sont validés côté service (valueOf sur les enums
 * {@link com.discipolat.modules.tickets.domain.Ticket.Categorie} et
 * {@link com.discipolat.modules.tickets.domain.Ticket.Priorite}).
 */
public record CreateTicketRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne peut dépasser 200 caractères")
        String titre,

        @Size(max = 10000, message = "La description ne peut dépasser 10 000 caractères")
        String description,

        @NotBlank(message = "La catégorie est obligatoire")
        String categorie,

        @NotBlank(message = "La priorité est obligatoire")
        String priorite
) {
}
