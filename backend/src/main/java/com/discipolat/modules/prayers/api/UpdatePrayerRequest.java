package com.discipolat.modules.prayers.api;

public record UpdatePrayerRequest(
        String titre,
        String description,
        String categorie,
        String priorite,
        String visibilite
) {}
