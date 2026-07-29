package com.discipolat.modules.events.api;

import java.time.LocalDateTime;

public record UpdateEventRequest(
        String titre,
        String description,
        String lieu,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer limitePlaces,
        String typeEvenement,
        String statut,
        String compteRendu
) {}
