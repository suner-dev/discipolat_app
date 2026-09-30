package com.discipolat.modules.events.api;

import java.time.LocalDateTime;

/**
 * Mise à jour PARTIELLE d'un événement.
 *
 * <p>Semantique : un composant {@code null} signifie « non fourni » et ne doit
 * pas écraser la valeur existante. C'est pourquoi les options booleanes sont
 * des {@link Boolean} et non des {@code boolean} : un primitif serait déballé
 * en {@code null} (NPE) ou, pire, réécrirait {@code false} par défaut.
 */
public record UpdateEventRequest(
        String titre,
        String description,
        String lieu,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer limitePlaces,
        String typeEvenement,
        String statut,
        String compteRendu,
        java.util.UUID departmentId,
        java.util.List<java.util.UUID> fichierIds,
        // --- options (V200) ---
        String imageUrl,
        java.util.List<String> tags,
        Boolean isPublic,
        Boolean requiresRegistration,
        Boolean hasCheckin,
        java.util.UUID streamId,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude,
        Integer geofenceRadiusMeters
) {}
